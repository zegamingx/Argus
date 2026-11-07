package com.argus.agi.platform.android.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import android.provider.OpenableColumns
import com.argus.agi.core.training.CanonicalDatasetDigests
import com.argus.agi.core.training.DatasetSplitMembership
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

enum class DatasetPurpose(val manifestValue: String) {
    MODEL_TRAINING("model_training"),
    TOKENIZER_FREQUENCY_FITTING("tokenizer_frequency_fitting"),
    ALIGNED_TOKENIZER_DEVELOPMENT("aligned_tokenizer_development")
}

data class CorpusImportPreview(
    val stageId: String,
    val corpusId: String,
    val selectedSourceCount: Int,
    val parsedDocumentCount: Int,
    val acceptedRecordCount: Long,
    val duplicateRecordCount: Long,
    val rejectedRecordCount: Long,
    val skippedFileCount: Int,
    val importedBytes: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val warnings: List<String>
)

data class LocalCorpusSummary(
    val corpusId: String,
    val revision: Int,
    val importedAtEpochMillis: Long,
    val recordCount: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val purposes: Set<DatasetPurpose>,
    val lastAdmissionDecision: String = "UNKNOWN"
)

data class LocalCorpusIntegrityReport(
    val corpusId: String,
    val revision: Int,
    val manifestSha256: String,
    val canonicalDatasetSha256: String,
    val splitManifestSha256: String,
    val recordCount: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val lastAdmissionDecision: String,
    val verifiedAtEpochMillis: Long
)

/**
 * Streams local sources into app-private staging. Text is decoded incrementally,
 * ZIP entries are processed directly from ZipInputStream, and deduplication is
 * disk-backed. No source file or archive entry is loaded as a whole byte array.
 */
class LocalDatasetImporter(context: Context) {

    private val appContext: Context = context.applicationContext

    suspend fun stage(
        uris: List<Uri>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        require(uris.size <= MAX_SELECTED_SOURCES) {
            "The import accepts at most " + MAX_SELECTED_SOURCES + " selected files at once."
        }
        val sources = ArrayList<ImportSource>(uris.size)
        for (uri in uris) {
            sources.add(
                ImportSource(
                    displayName = queryDisplayName(uri),
                    mimeType = appContext.contentResolver.getType(uri),
                    openStream = { appContext.contentResolver.openInputStream(uri) }
                )
            )
        }
        return stageSources(sources, purposes)
    }

    suspend fun stageBundledArgusId(purposes: Set<DatasetPurpose>): CorpusImportPreview {
        return stageSources(
            listOf(
                ImportSource(
                    displayName = "ArgusID.txt",
                    mimeType = "text/plain",
                    openStream = { appContext.assets.open(BUNDLED_ARGUS_ID_ASSET) }
                )
            ),
            purposes
        )
    }

    suspend fun stageBundledDigiTransId(purposes: Set<DatasetPurpose>): CorpusImportPreview {
        return stageSources(
            listOf(
                ImportSource(
                    displayName = "DigiTransID.zip",
                    mimeType = "application/zip",
                    openStream = { appContext.assets.open(BUNDLED_DIGITRANSID_ASSET) }
                )
            ),
            purposes
        )
    }

    private suspend fun stageSources(
        sources: List<ImportSource>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        require(sources.isNotEmpty()) { "Select at least one local dataset file." }
        require(purposes.isNotEmpty()) { "Select at least one dataset purpose." }
        require(sources.size <= MAX_SELECTED_SOURCES) {
            "The import accepts at most " + MAX_SELECTED_SOURCES + " selected files at once."
        }

        val stageId = UUID.randomUUID().toString()
        val corpusId = UUID.randomUUID().toString()
        val stageDirectory = File(File(appContext.filesDir, STAGING_DIRECTORY), stageId)
        if (!stageDirectory.mkdirs() && !stageDirectory.isDirectory) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }

        try {
            return stageInto(sources, purposes, stageId, corpusId, stageDirectory)
        } catch (exception: Exception) {
            stageDirectory.deleteRecursively()
            throw exception
        }
    }

    fun commit(stageId: String): LocalCorpusSummary {
        require(isValidId(stageId)) { "The staged corpus identifier is invalid." }
        val stageDirectory = File(File(appContext.filesDir, STAGING_DIRECTORY), stageId)
        val manifestFile = File(stageDirectory, MANIFEST_FILE)
        if (!stageDirectory.isDirectory || !manifestFile.isFile) {
            throw IllegalStateException("The staged corpus is no longer available. Import the files again.")
        }

        val manifestText = manifestFile.readText(Charsets.UTF_8)
        val manifest = JSONObject(manifestText)
        val corpusId = manifest.optString("corpusId")
        if (!isValidId(corpusId)) {
            throw IllegalStateException("The staged corpus manifest is invalid.")
        }
        verifyStagedImport(stageDirectory, manifestText, manifest)

        val corpusRoot = File(File(appContext.filesDir, CORPORA_DIRECTORY), corpusId)
        if (!corpusRoot.exists() && !corpusRoot.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus directory.")
        }

        val revisionDirectory = File(corpusRoot, "revision-000001")
        if (revisionDirectory.exists()) {
            throw IllegalStateException("This corpus revision already exists.")
        }
        if (!stageDirectory.renameTo(revisionDirectory)) {
            throw IllegalStateException("Could not commit the staged corpus to local storage.")
        }
        return summaryFromManifest(manifest, INITIAL_ADMISSION_DECISION)
    }

    /**
     * Recomputes the committed revision's canonical record, split and manifest identities.
     *
     * This operation streams the complete records file. Call it from Dispatchers.IO,
     * and require its successful report before a future corpus consumer opens records.
     * Integrity verification does not approve the admission decision or authorize training.
     */
    fun verifyCommittedCorpusIntegrity(
        corpusId: String,
        revision: Int
    ): LocalCorpusIntegrityReport {
        require(isValidId(corpusId)) { "The corpus identifier is invalid." }
        require(revision > 0) { "The corpus revision must be positive." }

        val revisionName = "revision-" + revision.toString().padStart(6, '0')
        val revisionDirectory = File(
            File(File(appContext.filesDir, CORPORA_DIRECTORY), corpusId),
            revisionName
        )
        val manifestFile = File(revisionDirectory, MANIFEST_FILE)
        if (!revisionDirectory.isDirectory || !manifestFile.isFile) {
            throw IllegalArgumentException("The committed corpus revision does not exist.")
        }

        val manifestText = manifestFile.readText(Charsets.UTF_8)
        val manifest = JSONObject(manifestText)
        if (manifest.optString("corpusId") != corpusId ||
            manifest.optInt("revision", -1) != revision
        ) {
            throw IllegalStateException("The committed corpus identity does not match its storage path.")
        }

        verifyStagedImport(
            stageDirectory = revisionDirectory,
            manifestText = manifestText,
            manifest = manifest,
            requireSinglePendingAdmission = false
        )

        val admissionDecision = readLatestAdmissionDecision(
            revisionDirectory = revisionDirectory,
            manifestText = manifestText,
            manifest = manifest
        )
        val datasetIdentity = manifest.optJSONObject("datasetIdentity")
            ?: throw IllegalStateException("The committed dataset identity is missing.")
        val splitManifest = manifest.optJSONObject("splitManifest")
            ?: throw IllegalStateException("The committed split manifest is missing.")
        val counts = manifest.optJSONObject("counts")
            ?: throw IllegalStateException("The committed corpus counts are missing.")
        val manifestSha256 = CanonicalDatasetDigests.sha256Hex(
            manifestText.toByteArray(StandardCharsets.UTF_8)
        )

        return LocalCorpusIntegrityReport(
            corpusId = corpusId,
            revision = revision,
            manifestSha256 = manifestSha256,
            canonicalDatasetSha256 = datasetIdentity.optString("canonicalSha256"),
            splitManifestSha256 = splitManifest.optString("sha256"),
            recordCount = datasetIdentity.optLong("recordCount", -1L),
            trainRecordCount = counts.optLong("trainRecords", -1L),
            validationRecordCount = counts.optLong("validationRecords", -1L),
            testRecordCount = counts.optLong("testRecords", -1L),
            lastAdmissionDecision = admissionDecision,
            verifiedAtEpochMillis = System.currentTimeMillis()
        )
    }

    fun discard(stageId: String) {
        require(isValidId(stageId)) { "The staged corpus identifier is invalid." }
        val stageDirectory = File(File(appContext.filesDir, STAGING_DIRECTORY), stageId)
        if (stageDirectory.exists()) {
            stageDirectory.deleteRecursively()
        }
    }

    fun listCommitted(): List<LocalCorpusSummary> {
        val corpusRoot = File(appContext.filesDir, CORPORA_DIRECTORY)
        val corpusDirectories = corpusRoot.listFiles() ?: return emptyList()
        return corpusDirectories
            .asSequence()
            .filter { it.isDirectory && isValidId(it.name) }
            .flatMap { corpusDirectory ->
                (corpusDirectory.listFiles() ?: emptyArray())
                    .asSequence()
                    .filter { it.isDirectory && it.name.startsWith("revision-") }
            }
            .mapNotNull { revisionDirectory ->
                val manifestFile = File(revisionDirectory, MANIFEST_FILE)
                if (!manifestFile.isFile) {
                    null
                } else {
                    runCatching {
                        val manifestText = manifestFile.readText(Charsets.UTF_8)
                        val manifest = JSONObject(manifestText)
                        val admissionDecision = readLatestAdmissionDecision(
                            revisionDirectory = revisionDirectory,
                            manifestText = manifestText,
                            manifest = manifest
                        )
                        summaryFromManifest(manifest, admissionDecision)
                    }.getOrNull()
                }
            }
            .sortedByDescending { it.importedAtEpochMillis }
            .toList()
    }

    private suspend fun stageInto(
        sources: List<ImportSource>,
        purposes: Set<DatasetPurpose>,
        stageId: String,
        corpusId: String,
        stageDirectory: File
    ): CorpusImportPreview {
        val rawRecordsFile = File(stageDirectory, RAW_RECORDS_FILE)
        val finalRecordsFile = File(stageDirectory, RECORDS_FILE)
        val sourceMetadata = JSONArray()
        val documentMetadata = linkedMapOf<String, DocumentMetadata>()
        val warnings = mutableListOf<String>()
        val storageGuard = StorageGuard(stageDirectory)

        var acceptedRecords = 0L
        var duplicateRecords = 0L
        var rejectedRecords = 0L
        var skippedFiles = 0
        var importedBytes = 0L
        var archiveEntryCount = 0

        val deduplicationDatabaseFile = File(stageDirectory, DEDUPLICATION_DATABASE_FILE)
        val deduplicationDatabase = SQLiteDatabase.openOrCreateDatabase(
            deduplicationDatabaseFile,
            null
        )
        var insertDigest: SQLiteStatement? = null
        var transactionStarted = false

        try {
            deduplicationDatabase.execSQL(
                "CREATE TABLE record_digests (digest TEXT NOT NULL PRIMARY KEY)"
            )
            insertDigest = deduplicationDatabase.compileStatement(
                "INSERT OR IGNORE INTO record_digests (digest) VALUES (?)"
            )
            deduplicationDatabase.beginTransaction()
            transactionStarted = true

            val digestStatement = insertDigest
                ?: throw IllegalStateException("The corpus deduplication statement is unavailable.")

            rawRecordsFile.bufferedWriter(Charsets.UTF_8).use { rawWriter ->
                for ((sourceIndex, source) in sources.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val sourceName = source.displayName
                    val archive = isZip(sourceName, source.mimeType)

                    if (archive) {
                        val archiveFile = File(
                            stageDirectory,
                            "archive-" + sourceIndex.toString().padStart(3, '0') + ".zip"
                        )
                        val sourceInfo = source.openStream()?.use { input ->
                            spoolArchive(input, archiveFile, storageGuard)
                        } ?: throw IllegalStateException(
                            "Could not open the selected file: " + sourceName
                        )

                        sourceMetadata.put(
                            JSONObject()
                                .put("name", sourceName)
                                .put("mimeType", source.mimeType ?: JSONObject.NULL)
                                .put("sizeBytes", sourceInfo.sizeBytes)
                                .put("sha256", sourceInfo.sha256)
                                .put("archive", true)
                        )

                        val archiveResult = processArchive(
                            archiveFile = archiveFile,
                            sourceName = sourceName,
                            sourceArtifactSha256 = sourceInfo.sha256,
                            startingEntryCount = archiveEntryCount,
                            stageDirectory = stageDirectory,
                            rawWriter = rawWriter,
                            insertDigest = digestStatement,
                            documentMetadata = documentMetadata,
                            warnings = warnings,
                            storageGuard = storageGuard
                        )
                        archiveEntryCount = archiveResult.entryCount
                        acceptedRecords += archiveResult.acceptedRecords
                        duplicateRecords += archiveResult.duplicateRecords
                        rejectedRecords += archiveResult.rejectedRecords
                        skippedFiles += archiveResult.skippedFiles
                        importedBytes += archiveResult.importedBytes
                        archiveFile.delete()
                    } else if (!isSupportedTextFile(sourceName)) {
                        skippedFiles += 1
                        val digest = MessageDigest.getInstance("SHA-256")
                        val sourceInput = source.openStream()
                            ?: throw IllegalStateException(
                                "Could not open the selected file: " + sourceName
                            )
                        val countedInput = CountingInputStream(
                            DigestInputStream(sourceInput, digest)
                        )
                        try {
                            drainInput(countedInput)
                        } finally {
                            countedInput.close()
                        }
                        val sourceSizeBytes = countedInput.bytesRead
                        sourceMetadata.put(
                            JSONObject()
                                .put("name", sourceName)
                                .put("mimeType", source.mimeType ?: JSONObject.NULL)
                                .put("sizeBytes", sourceSizeBytes)
                                .put("sha256", digestToHex(digest.digest()))
                                .put("archive", false)
                                .put("status", "skipped_unsupported_type")
                        )
                        addWarning(
                            warnings,
                            "Skipped " + sourceName +
                                " because only TXT, Markdown, JSONL, NDJSON and ZIP are supported."
                        )
                    } else {
                        val parsedFile = File(
                            stageDirectory,
                            "parsed-source-" + sourceIndex.toString().padStart(3, '0') + ".jsonl"
                        )
                        val digest = MessageDigest.getInstance("SHA-256")
                        val sourceInput = source.openStream()
                            ?: throw IllegalStateException(
                                "Could not open the selected file: " + sourceName
                            )
                        val countedInput = CountingInputStream(
                            DigestInputStream(sourceInput, digest)
                        )
                        val parsed = try {
                            parseDocument(
                                input = countedInput,
                                extension = extensionOf(sourceName),
                                displayName = sourceName,
                                parsedRecordsFile = parsedFile,
                                warnings = warnings,
                                storageGuard = storageGuard
                            )
                        } finally {
                            countedInput.close()
                        }
                        val sourceDigest = digestToHex(digest.digest())
                        sourceMetadata.put(
                            JSONObject()
                                .put("name", sourceName)
                                .put("mimeType", source.mimeType ?: JSONObject.NULL)
                                .put("sizeBytes", countedInput.bytesRead)
                                .put("sha256", sourceDigest)
                                .put("archive", false)
                        )
                        importedBytes += countedInput.bytesRead

                        val documentResult = if (parsed.isValid) {
                            val parsedRecords = parsed.file
                                ?: throw IllegalStateException("The parsed document file is missing.")
                            try {
                                ingestParsedRecords(
                                    parsedRecordsFile = parsedRecords,
                                    sourceDocumentDigest = sourceDigest,
                                    sourceArtifactSha256 = sourceDigest,
                                    displayName = sourceName,
                                    mediaType = source.mimeType,
                                    sourceSizeBytes = countedInput.bytesRead,
                                    rawWriter = rawWriter,
                                    insertDigest = digestStatement,
                                    documentMetadata = documentMetadata,
                                    storageGuard = storageGuard
                                )
                            } finally {
                                parsedRecords.delete()
                            }
                        } else {
                            IngestResult(0L, 0L, 0L, false)
                        }

                        acceptedRecords += documentResult.acceptedRecords
                        duplicateRecords += documentResult.duplicateRecords
                        rejectedRecords += parsed.rejectedRecords + documentResult.rejectedRecords
                        if (!documentResult.documentAccepted) {
                            skippedFiles += 1
                            addWarning(
                                warnings,
                                "No usable records were found in " + sourceName + "."
                            )
                        }
                    }
                }
            }

            deduplicationDatabase.setTransactionSuccessful()
        } finally {
            try {
                if (transactionStarted) {
                    deduplicationDatabase.endTransaction()
                }
            } finally {
                insertDigest?.close()
                deduplicationDatabase.close()
                deduplicationDatabaseFile.delete()
            }
        }

        if (acceptedRecords == 0L || documentMetadata.isEmpty()) {
            throw IllegalArgumentException(
                "No usable text records were found. Supported formats are TXT, Markdown, JSONL, NDJSON and ZIP."
            )
        }

        val splitEligibleDocumentIds = documentMetadata.values
            .filter { it.recordCount > 0L }
            .map { it.digest }
        val splitByDocument = createDocumentSplits(splitEligibleDocumentIds, warnings)
        val finalRecords = writeFinalRecords(
            rawRecordsFile = rawRecordsFile,
            finalRecordsFile = finalRecordsFile,
            splitByDocument = splitByDocument,
            storageGuard = storageGuard
        )
        rawRecordsFile.delete()

        val documentsJson = JSONArray()
        documentMetadata.values.forEach { document ->
            documentsJson.put(
                JSONObject()
                    .put("name", document.displayName)
                    .put("sha256", document.digest)
                    .put("sourceArtifactSha256", document.sourceArtifactSha256)
                    .put("sizeBytes", document.sizeBytes)
                    .put("mediaType", document.mediaType ?: JSONObject.NULL)
                    .put("recordCount", document.recordCount)
                    .put(
                        "split",
                        splitByDocument[document.digest] ?: EXCLUDED_DUPLICATE_ONLY_SPLIT
                    )
            )
        }

        val splitMembershipsJson = JSONObject()
        for (split in CanonicalDatasetDigests.SPLIT_NAMES) {
            val membership = finalRecords.memberships[split]
                ?: throw IllegalStateException("A split membership digest is missing: " + split)
            splitMembershipsJson.put(
                split,
                JSONObject()
                    .put("recordCount", membership.recordCount)
                    .put("sha256", membership.sha256)
            )
        }

        val splitPolicy = JSONObject()
            .put("algorithm", SPLIT_ALGORITHM)
            .put("seed", SPLIT_SEED)
            .put("grouping", "source-document")
            .put("validationRatio", VALIDATION_RATIO)
            .put("testRatio", TEST_RATIO)

        val splitManifest = JSONObject()
            .put("schemaVersion", CanonicalDatasetDigests.SPLIT_MANIFEST_SCHEMA_VERSION)
            .put("parentDatasetSha256", finalRecords.canonicalDatasetSha256)
            .put("policy", splitPolicy)
            .put("memberships", splitMembershipsJson)
            .put("sha256", finalRecords.splitManifestSha256)

        val manifest = JSONObject()
            .put("schemaVersion", MANIFEST_SCHEMA_VERSION)
            .put("corpusId", corpusId)
            .put("revision", 1)
            .put("importedAtEpochMillis", System.currentTimeMillis())
            .put("importerVersion", IMPORTER_VERSION)
            .put("recordFormat", CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID)
            .put("purposes", JSONArray(purposes.map { it.manifestValue }.sorted()))
            .put("sources", sourceMetadata)
            .put("documents", documentsJson)
            .put(
                "processing",
                JSONObject()
                    .put("parserVersion", PARSER_VERSION)
                    .put("normalizationVersion", NORMALIZATION_VERSION)
                    .put("deduplicationVersion", DEDUPLICATION_VERSION)
            )
            .put(
                "tokenization",
                JSONObject()
                    .put("status", "NOT_TOKENIZED")
                    .put("tokenizerProfileId", JSONObject.NULL)
            )
            .put(
                "datasetIdentity",
                JSONObject()
                    .put("algorithm", "SHA-256")
                    .put("canonicalFormat", CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID)
                    .put("canonicalSha256", finalRecords.canonicalDatasetSha256)
                    .put("recordCount", finalRecords.counts.total)
            )
            .put("splitManifest", splitManifest)
            .put(
                "admissionDecisionLog",
                JSONObject()
                    .put("format", "jsonl")
                    .put("path", ADMISSION_DECISIONS_FILE)
                    .put("appendOnly", true)
            )
            .put("counts", JSONObject()
                .put("acceptedRecords", acceptedRecords)
                .put("duplicateRecords", duplicateRecords)
                .put("rejectedRecords", rejectedRecords)
                .put("skippedFiles", skippedFiles)
                .put("importedBytes", importedBytes)
                .put("trainRecords", finalRecords.counts.train)
                .put("validationRecords", finalRecords.counts.validation)
                .put("testRecords", finalRecords.counts.test)
            )
            .put("split", splitPolicy)
            .put("warnings", JSONArray(warnings))

        val manifestContent = manifest.toString(2)
        File(stageDirectory, MANIFEST_FILE).writeText(manifestContent, Charsets.UTF_8)
        val manifestSha256 = CanonicalDatasetDigests.sha256Hex(
            manifestContent.toByteArray(StandardCharsets.UTF_8)
        )
        File(stageDirectory, MANIFEST_DIGEST_FILE).writeText(
            manifestSha256 + "\n",
            Charsets.UTF_8
        )
        writeInitialAdmissionDecision(
            stageDirectory = stageDirectory,
            manifestSha256 = manifestSha256,
            datasetSha256 = finalRecords.canonicalDatasetSha256
        )

        return CorpusImportPreview(
            stageId = stageId,
            corpusId = corpusId,
            selectedSourceCount = sources.size,
            parsedDocumentCount = documentMetadata.size,
            acceptedRecordCount = acceptedRecords,
            duplicateRecordCount = duplicateRecords,
            rejectedRecordCount = rejectedRecords,
            skippedFileCount = skippedFiles,
            importedBytes = importedBytes,
            trainRecordCount = finalRecords.counts.train,
            validationRecordCount = finalRecords.counts.validation,
            testRecordCount = finalRecords.counts.test,
            warnings = warnings.toList()
        )
    }

    private suspend fun spoolArchive(
        input: InputStream,
        targetFile: File,
        storageGuard: StorageGuard
    ): SourceInfo {
        val digest = MessageDigest.getInstance("SHA-256")
        var sizeBytes = 0L
        val digestInput = DigestInputStream(input, digest)
        BufferedOutputStream(FileOutputStream(targetFile), STREAM_BUFFER_SIZE).use { output ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val readCount = digestInput.read(buffer)
                if (readCount < 0) break
                storageGuard.beforeWrite(readCount.toLong())
                output.write(buffer, 0, readCount)
                sizeBytes += readCount.toLong()
            }
        }
        return SourceInfo(sizeBytes, digestToHex(digest.digest()))
    }

    private suspend fun processArchive(
        archiveFile: File,
        sourceName: String,
        sourceArtifactSha256: String,
        startingEntryCount: Int,
        stageDirectory: File,
        rawWriter: BufferedWriter,
        insertDigest: SQLiteStatement,
        documentMetadata: MutableMap<String, DocumentMetadata>,
        warnings: MutableList<String>,
        storageGuard: StorageGuard
    ): ArchiveResult {
        var entryCount = startingEntryCount
        var acceptedRecords = 0L
        var duplicateRecords = 0L
        var rejectedRecords = 0L
        var skippedFiles = 0
        var importedBytes = 0L

        FileInputStream(archiveFile).use { fileInput ->
            ZipInputStream(
                BufferedInputStream(fileInput, STREAM_BUFFER_SIZE)
            ).use { zip ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry: ZipEntry = zip.nextEntry ?: break
                    entryCount += 1
                    if (entryCount > MAX_ARCHIVE_ENTRIES) {
                        throw IllegalArgumentException(
                            "The selected ZIP files contain more than " +
                                MAX_ARCHIVE_ENTRIES + " entries in total."
                        )
                    }

                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }

                    val entryName = entry.name
                    if (!isSafeArchivePath(entryName)) {
                        drainZipEntry(zip)
                        skippedFiles += 1
                        addWarning(warnings, "Skipped an unsafe archive path.")
                        zip.closeEntry()
                        continue
                    }

                    if (!isSupportedTextFile(entryName)) {
                        drainZipEntry(zip)
                        skippedFiles += 1
                        addWarning(
                            warnings,
                            "Skipped unsupported archive entry: " + entryName
                        )
                        zip.closeEntry()
                        continue
                    }

                    val entryIndex = entryCount.toString().padStart(5, '0')
                    val parsedRecordsFile = File(
                        stageDirectory,
                        "parsed-archive-entry-" + entryIndex + ".jsonl"
                    )
                    val entryDigest = MessageDigest.getInstance("SHA-256")
                    val countedEntryInput = CountingInputStream(
                        DigestInputStream(zip, entryDigest)
                    )
                    val parsed = parseDocument(
                        input = countedEntryInput,
                        extension = extensionOf(entryName),
                        displayName = sourceName + "!/" + entryName,
                        parsedRecordsFile = parsedRecordsFile,
                        warnings = warnings,
                        storageGuard = storageGuard
                    )
                    val entrySizeBytes = countedEntryInput.bytesRead
                    val documentDigest = digestToHex(entryDigest.digest())
                    importedBytes += entrySizeBytes

                    val displayName = sourceName + "!/" + entryName
                    val documentResult = if (parsed.isValid) {
                        val parsedRecords = parsed.file
                            ?: throw IllegalStateException("The parsed archive entry file is missing.")
                        try {
                            ingestParsedRecords(
                                parsedRecordsFile = parsedRecords,
                                sourceDocumentDigest = documentDigest,
                                sourceArtifactSha256 = sourceArtifactSha256,
                                displayName = displayName,
                                mediaType = mimeTypeForName(entryName),
                                sourceSizeBytes = entrySizeBytes,
                                rawWriter = rawWriter,
                                insertDigest = insertDigest,
                                documentMetadata = documentMetadata,
                                storageGuard = storageGuard
                            )
                        } finally {
                            parsedRecords.delete()
                        }
                    } else {
                        IngestResult(0L, 0L, 0L, false)
                    }

                    acceptedRecords += documentResult.acceptedRecords
                    duplicateRecords += documentResult.duplicateRecords
                    rejectedRecords += parsed.rejectedRecords + documentResult.rejectedRecords
                    if (!documentResult.documentAccepted) {
                        skippedFiles += 1
                        addWarning(warnings, "No usable records were found in " + displayName + ".")
                    }
                    zip.closeEntry()
                }
            }
        }

        return ArchiveResult(
            entryCount = entryCount,
            acceptedRecords = acceptedRecords,
            duplicateRecords = duplicateRecords,
            rejectedRecords = rejectedRecords,
            skippedFiles = skippedFiles,
            importedBytes = importedBytes
        )
    }

    private suspend fun parseDocument(
        input: CountingInputStream,
        extension: String,
        displayName: String,
        parsedRecordsFile: File,
        warnings: MutableList<String>,
        storageGuard: StorageGuard
    ): ParsedFile {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val reader = BufferedReader(
            InputStreamReader(input, decoder),
            STREAM_BUFFER_SIZE
        )

        return try {
            var rejectedRecords = 0L
            parsedRecordsFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                rejectedRecords = when (extension) {
                    "txt", "md" -> parsePlainText(reader, writer, storageGuard)
                    "jsonl", "ndjson" -> parseJsonLines(
                        reader = reader,
                        writer = writer,
                        displayName = displayName,
                        warnings = warnings,
                        storageGuard = storageGuard
                    )
                    else -> 1L
                }
            }
            ParsedFile(parsedRecordsFile, true, rejectedRecords)
        } catch (exception: CharacterCodingException) {
            drainInput(input)
            parsedRecordsFile.delete()
            addWarning(warnings, "Skipped " + displayName + " because it is not valid UTF-8.")
            ParsedFile(null, false, 1L)
        }
    }

    private suspend fun parsePlainText(
        reader: Reader,
        writer: BufferedWriter,
        storageGuard: StorageGuard
    ): Long {
        val paragraph = StringBuilder()
        val buffer = CharArray(STREAM_BUFFER_SIZE)
        var lineHasContent = false
        var previousWasCarriageReturn = false

        suspend fun emitWholeParagraph() {
            if (paragraph.isEmpty()) return
            writeCandidate(paragraph.toString(), writer, storageGuard)
            paragraph.setLength(0)
        }

        suspend fun emitLongParagraphChunk() {
            if (paragraph.length <= MAX_TEXT_RECORD_CHARACTERS) return
            var splitAt = MAX_TEXT_RECORD_CHARACTERS
            val minimumBoundary = MAX_TEXT_RECORD_CHARACTERS * 3 / 4
            while (splitAt > minimumBoundary && !paragraph[splitAt - 1].isWhitespace()) {
                splitAt -= 1
            }
            if (splitAt <= minimumBoundary) {
                splitAt = MAX_TEXT_RECORD_CHARACTERS
            }
            if (
                splitAt < paragraph.length &&
                Character.isHighSurrogate(paragraph[splitAt - 1]) &&
                Character.isLowSurrogate(paragraph[splitAt])
            ) {
                splitAt -= 1
            }
            val chunk = paragraph.substring(0, splitAt)
            paragraph.delete(0, splitAt)
            writeCandidate(chunk, writer, storageGuard)
        }

        suspend fun completeLine() {
            if (lineHasContent) {
                paragraph.append('\n')
                lineHasContent = false
            } else {
                emitWholeParagraph()
            }
        }

        while (true) {
            val readCount = reader.read(buffer)
            if (readCount < 0) break
            currentCoroutineContext().ensureActive()

            for (index in 0 until readCount) {
                val character = buffer[index]
                when (character) {
                    '\r' -> {
                        completeLine()
                        previousWasCarriageReturn = true
                    }
                    '\n' -> {
                        if (previousWasCarriageReturn) {
                            previousWasCarriageReturn = false
                        } else {
                            completeLine()
                        }
                    }
                    else -> {
                        previousWasCarriageReturn = false
                        paragraph.append(character)
                        if (!character.isWhitespace()) {
                            lineHasContent = true
                        }
                        emitLongParagraphChunk()
                    }
                }
            }
        }

        if (lineHasContent || paragraph.isNotEmpty()) {
            emitWholeParagraph()
        }
        return 0L
    }

    private suspend fun parseJsonLines(
        reader: Reader,
        writer: BufferedWriter,
        displayName: String,
        warnings: MutableList<String>,
        storageGuard: StorageGuard
    ): Long {
        val line = StringBuilder()
        val buffer = CharArray(STREAM_BUFFER_SIZE)
        var oversizedLine = false
        var previousWasCarriageReturn = false
        var invalidLines = 0L

        suspend fun emitLine() {
            if (oversizedLine) {
                invalidLines += 1L
            } else if (line.isNotBlank()) {
                val extracted = extractJsonLine(line.toString())
                if (extracted.isNullOrBlank()) {
                    invalidLines += 1L
                } else {
                    writeCandidate(extracted, writer, storageGuard)
                }
            }
            line.setLength(0)
            oversizedLine = false
        }

        while (true) {
            val readCount = reader.read(buffer)
            if (readCount < 0) break
            currentCoroutineContext().ensureActive()

            for (index in 0 until readCount) {
                val character = buffer[index]
                when (character) {
                    '\r' -> {
                        emitLine()
                        previousWasCarriageReturn = true
                    }
                    '\n' -> {
                        if (previousWasCarriageReturn) {
                            previousWasCarriageReturn = false
                        } else {
                            emitLine()
                        }
                    }
                    else -> {
                        previousWasCarriageReturn = false
                        if (!oversizedLine) {
                            if (line.length < MAX_JSONL_LINE_CHARACTERS) {
                                line.append(character)
                            } else {
                                oversizedLine = true
                            }
                        }
                    }
                }
            }
        }

        if (line.isNotEmpty() || oversizedLine) {
            emitLine()
        }
        if (invalidLines > 0L) {
            addWarning(
                warnings,
                displayName + " contains " + invalidLines +
                    " empty, oversized or unsupported JSONL rows."
            )
        }
        return invalidLines
    }

    private fun extractJsonLine(source: String): String? {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) return null

        return runCatching {
            val value = JSONTokener(trimmed).nextValue()
            when (value) {
                is JSONObject -> {
                    val fields = listOf(
                        "text",
                        "content",
                        "body",
                        "article",
                        "document",
                        "prompt",
                        "completion",
                        "response",
                        "answer",
                        "input",
                        "output"
                    )
                    fields.mapNotNull { key ->
                        val field = value.opt(key)
                        (field as? String)?.takeIf { it.isNotBlank() }
                    }.distinct().joinToString("\n").takeIf { it.isNotBlank() }
                }
                is String -> value
                else -> null
            }
        }.getOrNull()
    }

    private suspend fun writeCandidate(
        source: String,
        writer: BufferedWriter,
        storageGuard: StorageGuard
    ) {
        val normalized = normalizeText(source)
        if (normalized.isBlank()) return

        val serializedRecord = JSONObject().put("text", normalized).toString()
        storageGuard.beforeWrite(serializedRecord.length.toLong() * 2L)
        writer.write(serializedRecord)
        writer.newLine()
    }

    private suspend fun ingestParsedRecords(
        parsedRecordsFile: File,
        sourceDocumentDigest: String,
        sourceArtifactSha256: String,
        displayName: String,
        mediaType: String?,
        sourceSizeBytes: Long,
        rawWriter: BufferedWriter,
        insertDigest: SQLiteStatement,
        documentMetadata: MutableMap<String, DocumentMetadata>,
        storageGuard: StorageGuard
    ): IngestResult {
        var accepted = 0L
        var duplicates = 0L
        var rejected = 0L
        var processedSinceCancellationCheck = 0

        parsedRecordsFile.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                if (processedSinceCancellationCheck >= CANCELLATION_CHECK_RECORD_INTERVAL) {
                    currentCoroutineContext().ensureActive()
                    processedSinceCancellationCheck = 0
                }
                processedSinceCancellationCheck += 1

                val text = runCatching {
                    JSONObject(line).optString("text")
                }.getOrNull()
                if (text.isNullOrBlank()) {
                    rejected += 1L
                    continue
                }

                val recordDigest = sha256Text(text)
                insertDigest.clearBindings()
                insertDigest.bindString(1, recordDigest)
                if (insertDigest.executeInsert() < 0L) {
                    duplicates += 1L
                    continue
                }

                val record = JSONObject()
                    .put("recordId", recordDigest)
                    .put("sourceDocumentId", sourceDocumentDigest)
                    .put("sourceName", displayName)
                    .put("text", text)
                val serializedRecord = record.toString()
                storageGuard.beforeWrite(serializedRecord.length.toLong() * 2L)
                rawWriter.write(serializedRecord)
                rawWriter.newLine()
                accepted += 1L
            }
        }

        val existing = documentMetadata[sourceDocumentDigest]
        if (existing == null && (accepted > 0L || duplicates > 0L)) {
            documentMetadata[sourceDocumentDigest] = DocumentMetadata(
                digest = sourceDocumentDigest,
                sourceArtifactSha256 = sourceArtifactSha256,
                displayName = displayName,
                sizeBytes = sourceSizeBytes,
                mediaType = mediaType,
                recordCount = accepted
            )
        } else if (existing != null) {
            existing.recordCount += accepted
        }

        return IngestResult(
            acceptedRecords = accepted,
            duplicateRecords = duplicates,
            rejectedRecords = rejected,
            documentAccepted = accepted > 0L || duplicates > 0L
        )
    }

    private fun createDocumentSplits(
        documentIds: List<String>,
        warnings: MutableList<String>
    ): Map<String, String> {
        val ordered = documentIds.sortedBy { documentId ->
            sha256Text(SPLIT_SEED + ":" + documentId)
        }
        if (ordered.size < 3) {
            addWarning(
                warnings,
                "Fewer than three unique source documents were imported; all records remain in the training split."
            )
            return ordered.associateWith { "train" }
        }

        val validationCount = (ordered.size * VALIDATION_RATIO).roundToInt().coerceAtLeast(1)
        val testCount = (ordered.size * TEST_RATIO).roundToInt().coerceAtLeast(1)
        val boundedValidation = validationCount.coerceAtMost(ordered.size - 2)
        val boundedTest = testCount.coerceAtMost(ordered.size - boundedValidation - 1)
        val result = linkedMapOf<String, String>()
        ordered.take(boundedTest).forEach { result[it] = "test" }
        ordered.drop(boundedTest).take(boundedValidation).forEach { result[it] = "validation" }
        ordered.drop(boundedTest + boundedValidation).forEach { result[it] = "train" }
        return result
    }

    private suspend fun writeFinalRecords(
        rawRecordsFile: File,
        finalRecordsFile: File,
        splitByDocument: Map<String, String>,
        storageGuard: StorageGuard
    ): FinalRecordMetadata {
        var train = 0L
        var validation = 0L
        var test = 0L
        var processedSinceCancellationCheck = 0

        val datasetDigest = CanonicalDatasetDigests.newDatasetDigest()
        val membershipDigests = CanonicalDatasetDigests.SPLIT_NAMES.associateWith { split ->
            CanonicalDatasetDigests.newMembershipDigest(split)
        }

        rawRecordsFile.bufferedReader(Charsets.UTF_8).use { reader ->
            finalRecordsFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    if (processedSinceCancellationCheck >= CANCELLATION_CHECK_RECORD_INTERVAL) {
                        currentCoroutineContext().ensureActive()
                        processedSinceCancellationCheck = 0
                    }
                    processedSinceCancellationCheck += 1

                    val record = JSONObject(line)
                    val recordId = record.optString("recordId")
                    val documentId = record.optString("sourceDocumentId")
                    val sourceName = record.optString("sourceName")
                    val text = record.optString("text")
                    val split = splitByDocument[documentId]
                        ?: throw IllegalStateException(
                            "A corpus record references a source document without a split."
                        )

                    val canonicalLine = CanonicalDatasetDigests.canonicalRecordLine(
                        recordId = recordId,
                        sourceDocumentId = documentId,
                        sourceName = sourceName,
                        text = text,
                        split = split
                    )
                    CanonicalDatasetDigests.updateDatasetRecord(datasetDigest, canonicalLine)

                    val membershipDigest = membershipDigests[split]
                        ?: throw IllegalStateException("A split membership digest is unavailable.")
                    CanonicalDatasetDigests.updateMembershipDigest(
                        digest = membershipDigest,
                        recordId = recordId,
                        sourceDocumentId = documentId
                    )

                    storageGuard.beforeWrite(canonicalLine.length.toLong() * 2L + 1L)
                    writer.write(canonicalLine)
                    writer.write("\n")

                    when (split) {
                        "validation" -> validation += 1L
                        "test" -> test += 1L
                        "train" -> train += 1L
                        else -> throw IllegalStateException("The dataset split is not recognized.")
                    }
                }
            }
        }

        val counts = SplitCounts(train, validation, test)
        val canonicalDatasetSha256 = CanonicalDatasetDigests.finish(datasetDigest)
        val memberships = CanonicalDatasetDigests.SPLIT_NAMES.associateWith { split ->
            val digest = membershipDigests[split]
                ?: throw IllegalStateException("A split membership digest is unavailable.")
            DatasetSplitMembership(
                recordCount = when (split) {
                    "train" -> counts.train
                    "validation" -> counts.validation
                    "test" -> counts.test
                    else -> throw IllegalStateException("The dataset split is not recognized.")
                },
                sha256 = CanonicalDatasetDigests.finish(digest)
            )
        }
        val splitManifestSha256 = CanonicalDatasetDigests.splitManifestSha256(
            parentDatasetSha256 = canonicalDatasetSha256,
            algorithm = SPLIT_ALGORITHM,
            seed = SPLIT_SEED,
            grouping = "source-document",
            validationRatio = VALIDATION_RATIO,
            testRatio = TEST_RATIO,
            memberships = memberships
        )

        return FinalRecordMetadata(
            counts = counts,
            canonicalDatasetSha256 = canonicalDatasetSha256,
            memberships = memberships,
            splitManifestSha256 = splitManifestSha256
        )
    }

    private fun verifyStagedImport(
        stageDirectory: File,
        manifestText: String,
        manifest: JSONObject,
        requireSinglePendingAdmission: Boolean = true
    ) {
        val manifestSha256 = CanonicalDatasetDigests.sha256Hex(
            manifestText.toByteArray(StandardCharsets.UTF_8)
        )
        val digestFile = File(stageDirectory, MANIFEST_DIGEST_FILE)
        if (!digestFile.isFile ||
            digestFile.readText(Charsets.UTF_8).trim() != manifestSha256
        ) {
            throw IllegalStateException("The staged corpus manifest checksum is invalid.")
        }

        if (manifest.optInt("schemaVersion", -1) != MANIFEST_SCHEMA_VERSION ||
            manifest.optString("importerVersion") != IMPORTER_VERSION ||
            manifest.optString("recordFormat") != CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID ||
            !isValidId(manifest.optString("corpusId")) ||
            manifest.optInt("revision", -1) != 1 ||
            manifest.optLong("importedAtEpochMillis", 0L) <= 0L
        ) {
            throw IllegalStateException("The staged corpus format is unsupported. Import the files again.")
        }

        val processing = manifest.optJSONObject("processing")
            ?: throw IllegalStateException("The staged processing identity is missing.")
        if (processing.optString("parserVersion") != PARSER_VERSION ||
            processing.optString("normalizationVersion") != NORMALIZATION_VERSION ||
            processing.optString("deduplicationVersion") != DEDUPLICATION_VERSION
        ) {
            throw IllegalStateException("The staged processing pipeline version is unsupported.")
        }
        val tokenization = manifest.optJSONObject("tokenization")
            ?: throw IllegalStateException("The staged tokenization state is missing.")
        if (tokenization.optString("status") != "NOT_TOKENIZED" ||
            !tokenization.has("tokenizerProfileId") ||
            !tokenization.isNull("tokenizerProfileId")
        ) {
            throw IllegalStateException("A staged local corpus must remain un-tokenized.")
        }

        val purposes = manifest.optJSONArray("purposes")
            ?: throw IllegalStateException("The staged dataset purposes are missing.")
        if (purposes.length() == 0) {
            throw IllegalStateException("The staged dataset has no declared purpose.")
        }
        for (index in 0 until purposes.length()) {
            val purpose = purposes.optString(index)
            if (DatasetPurpose.values().none { it.manifestValue == purpose }) {
                throw IllegalStateException("The staged dataset contains an unsupported purpose.")
            }
        }

        val sources = manifest.optJSONArray("sources")
            ?: throw IllegalStateException("The staged source inventory is missing.")
        if (sources.length() == 0) {
            throw IllegalStateException("The staged dataset has no source artifacts.")
        }
        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index)
                ?: throw IllegalStateException("The staged source inventory is malformed.")
            val sourceDigest = source.opt("sha256")
            if (source.optString("name").isBlank() ||
                sourceDigest !is String ||
                !CanonicalDatasetDigests.isSha256Hex(sourceDigest) ||
                !source.has("sizeBytes") ||
                source.optLong("sizeBytes", -1L) < 0L ||
                source.opt("archive") !is Boolean
            ) {
                throw IllegalStateException("A staged source artifact identity is incomplete or malformed.")
            }
            val sourceStatus = source.optString("status")
            if (sourceStatus.isNotEmpty() && sourceStatus != "skipped_unsupported_type") {
                throw IllegalStateException("A staged source artifact has an unsupported import status.")
            }
        }

        val datasetIdentity = manifest.optJSONObject("datasetIdentity")
            ?: throw IllegalStateException("The staged dataset identity is missing.")
        val expectedDatasetSha256 = datasetIdentity.optString("canonicalSha256")
        if (datasetIdentity.optString("algorithm") != "SHA-256" ||
            datasetIdentity.optString("canonicalFormat") != CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID ||
            !CanonicalDatasetDigests.isSha256Hex(expectedDatasetSha256)
        ) {
            throw IllegalStateException("The staged canonical dataset digest is invalid.")
        }

        val splitManifest = manifest.optJSONObject("splitManifest")
            ?: throw IllegalStateException("The staged split manifest is missing.")
        if (splitManifest.optInt(
                "schemaVersion",
                -1
            ) != CanonicalDatasetDigests.SPLIT_MANIFEST_SCHEMA_VERSION
        ) {
            throw IllegalStateException("The staged split manifest version is unsupported.")
        }
        val splitPolicy = splitManifest.optJSONObject("policy")
            ?: throw IllegalStateException("The staged split policy is missing.")
        if (splitPolicy.optString("algorithm") != SPLIT_ALGORITHM ||
            splitPolicy.optString("seed") != SPLIT_SEED ||
            splitPolicy.optString("grouping") != "source-document" ||
            splitPolicy.optDouble("validationRatio", -1.0) != VALIDATION_RATIO ||
            splitPolicy.optDouble("testRatio", -1.0) != TEST_RATIO
        ) {
            throw IllegalStateException("The staged split policy is unsupported.")
        }
        val topLevelSplitPolicy = manifest.optJSONObject("split")
            ?: throw IllegalStateException("The staged top-level split policy is missing.")
        if (topLevelSplitPolicy.optString("algorithm") != splitPolicy.optString("algorithm") ||
            topLevelSplitPolicy.optString("seed") != splitPolicy.optString("seed") ||
            topLevelSplitPolicy.optString("grouping") != splitPolicy.optString("grouping") ||
            topLevelSplitPolicy.optDouble("validationRatio", -1.0) != VALIDATION_RATIO ||
            topLevelSplitPolicy.optDouble("testRatio", -1.0) != TEST_RATIO
        ) {
            throw IllegalStateException("The staged split policy declarations disagree.")
        }
        val expectedMemberships = splitManifest.optJSONObject("memberships")
            ?: throw IllegalStateException("The staged split memberships are missing.")
        if (expectedMemberships.length() != CanonicalDatasetDigests.SPLIT_NAMES.size) {
            throw IllegalStateException("The staged split manifest has an invalid membership set.")
        }
        val parentDatasetSha256 = splitManifest.optString("parentDatasetSha256")
        if (parentDatasetSha256 != expectedDatasetSha256) {
            throw IllegalStateException("The staged split manifest references a different dataset.")
        }

        val documents = manifest.optJSONArray("documents")
            ?: throw IllegalStateException("The staged document inventory is missing.")
        val documentSplits = linkedMapOf<String, String>()
        val expectedDocumentNames = linkedMapOf<String, String>()
        val expectedDocumentCounts = linkedMapOf<String, Long>()
        val sourceArtifactIds = linkedSetOf<String>()
        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index)
                ?: throw IllegalStateException("The staged source inventory is malformed.")
            sourceArtifactIds.add(source.getString("sha256"))
        }
        for (index in 0 until documents.length()) {
            val document = documents.optJSONObject(index)
                ?: throw IllegalStateException("The staged document inventory is malformed.")
            val documentId = document.optString("sha256")
            val sourceArtifactSha256 = document.optString("sourceArtifactSha256")
            val documentName = document.optString("name")
            val split = document.optString("split")
            val recordCount = document.optLong("recordCount", -1L)
            if (!CanonicalDatasetDigests.isSha256Hex(documentId) ||
                !CanonicalDatasetDigests.isSha256Hex(sourceArtifactSha256) ||
                !sourceArtifactIds.contains(sourceArtifactSha256) ||
                documentName.isBlank() ||
                document.optLong("sizeBytes", -1L) < 0L ||
                recordCount < 0L ||
                expectedDocumentCounts.put(documentId, recordCount) != null
            ) {
                throw IllegalStateException("The staged document identity or record count is invalid.")
            }
            expectedDocumentNames[documentId] = documentName
            if (recordCount == 0L) {
                if (split != EXCLUDED_DUPLICATE_ONLY_SPLIT) {
                    throw IllegalStateException("A duplicate-only source document has an invalid disposition.")
                }
            } else {
                if (split !in CanonicalDatasetDigests.SPLIT_NAMES ||
                    documentSplits.put(documentId, split) != null
                ) {
                    throw IllegalStateException("The staged document split is invalid.")
                }
            }
        }

        val datasetDigest = CanonicalDatasetDigests.newDatasetDigest()
        val membershipDigests = CanonicalDatasetDigests.SPLIT_NAMES.associateWith { split ->
            CanonicalDatasetDigests.newMembershipDigest(split)
        }
        val actualDocumentSplits = linkedMapOf<String, String>()
        val actualDocumentCounts = linkedMapOf<String, Long>()
        var trainCount = 0L
        var validationCount = 0L
        var testCount = 0L
        var totalCount = 0L

        val recordsFile = File(stageDirectory, RECORDS_FILE)
        if (!recordsFile.isFile) {
            throw IllegalStateException("The staged canonical records file is missing.")
        }
        recordsFile.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) {
                    throw IllegalStateException("The staged canonical records contain an empty line.")
                }
                val record = try {
                    JSONObject(line)
                } catch (exception: Exception) {
                    throw IllegalStateException("The staged canonical record is malformed.", exception)
                }
                val recordId = record.optString("recordId")
                val documentId = record.optString("sourceDocumentId")
                val sourceName = record.optString("sourceName")
                val text = record.optString("text")
                val split = record.optString("split")
                val expectedDocumentSplit = documentSplits[documentId]
                    ?: throw IllegalStateException("A record references an unknown source document.")
                if (expectedDocumentSplit != split) {
                    throw IllegalStateException("A source document appears in more than one split.")
                }
                if (!CanonicalDatasetDigests.isSha256Hex(documentId) ||
                    sourceName != expectedDocumentNames[documentId] ||
                    text.isBlank() ||
                    recordId != sha256Text(text) ||
                    normalizeText(text) != text
                ) {
                    throw IllegalStateException(
                        "A staged record ID, source identity or normalized text is invalid."
                    )
                }

                val canonicalLine = CanonicalDatasetDigests.canonicalRecordLine(
                    recordId = recordId,
                    sourceDocumentId = documentId,
                    sourceName = sourceName,
                    text = text,
                    split = split
                )
                if (line != canonicalLine) {
                    throw IllegalStateException("A staged record is not in canonical JSONL form.")
                }
                CanonicalDatasetDigests.updateDatasetRecord(datasetDigest, canonicalLine)

                val membershipDigest = membershipDigests[split]
                    ?: throw IllegalStateException("A staged record has an unsupported split.")
                CanonicalDatasetDigests.updateMembershipDigest(
                    digest = membershipDigest,
                    recordId = recordId,
                    sourceDocumentId = documentId
                )
                actualDocumentSplits[documentId] = split
                actualDocumentCounts[documentId] = (actualDocumentCounts[documentId] ?: 0L) + 1L

                when (split) {
                    "train" -> trainCount += 1L
                    "validation" -> validationCount += 1L
                    "test" -> testCount += 1L
                    else -> throw IllegalStateException("A staged record has an unsupported split.")
                }
                totalCount += 1L
            }
        }

        if (actualDocumentSplits.keys != documentSplits.keys) {
            throw IllegalStateException("The staged document inventory does not match its records.")
        }
        for ((documentId, expectedCount) in expectedDocumentCounts) {
            if (expectedCount == 0L) {
                if (actualDocumentCounts.containsKey(documentId)) {
                    throw IllegalStateException("A duplicate-only source document unexpectedly owns a record.")
                }
            } else if (actualDocumentCounts[documentId] != expectedCount) {
                throw IllegalStateException("A staged document record count is inconsistent.")
            }
        }

        val actualDatasetSha256 = CanonicalDatasetDigests.finish(datasetDigest)
        val exactRecordsFileSha256 = canonicalRecordsFileSha256(recordsFile)
        if (actualDatasetSha256 != exactRecordsFileSha256 ||
            actualDatasetSha256 != expectedDatasetSha256 ||
            datasetIdentity.optLong("recordCount", -1L) != totalCount
        ) {
            throw IllegalStateException("The staged canonical dataset digest or record count is invalid.")
        }

        val actualMemberships = CanonicalDatasetDigests.SPLIT_NAMES.associateWith { split ->
            val digest = membershipDigests[split]
                ?: throw IllegalStateException("A split membership digest is unavailable.")
            val membership = DatasetSplitMembership(
                recordCount = when (split) {
                    "train" -> trainCount
                    "validation" -> validationCount
                    "test" -> testCount
                    else -> throw IllegalStateException("The dataset split is not recognized.")
                },
                sha256 = CanonicalDatasetDigests.finish(digest)
            )
            val expected = expectedMemberships.optJSONObject(split)
                ?: throw IllegalStateException("A split membership entry is missing.")
            if (expected.optLong("recordCount", -1L) != membership.recordCount ||
                expected.optString("sha256") != membership.sha256
            ) {
                throw IllegalStateException("A staged split membership digest is invalid.")
            }
            membership
        }

        val counts = manifest.optJSONObject("counts")
            ?: throw IllegalStateException("The staged corpus counts are missing.")
        if (counts.optLong("acceptedRecords", -1L) != totalCount ||
            counts.optLong("trainRecords", -1L) != trainCount ||
            counts.optLong("validationRecords", -1L) != validationCount ||
            counts.optLong("testRecords", -1L) != testCount
        ) {
            throw IllegalStateException("The staged corpus split counts are inconsistent.")
        }

        val actualSplitManifestSha256 = CanonicalDatasetDigests.splitManifestSha256(
            parentDatasetSha256 = actualDatasetSha256,
            algorithm = splitPolicy.optString("algorithm"),
            seed = splitPolicy.optString("seed"),
            grouping = splitPolicy.optString("grouping"),
            validationRatio = splitPolicy.optDouble("validationRatio", -1.0),
            testRatio = splitPolicy.optDouble("testRatio", -1.0),
            memberships = actualMemberships
        )
        if (actualSplitManifestSha256 != splitManifest.optString("sha256")) {
            throw IllegalStateException("The staged split manifest checksum is invalid.")
        }

        if (requireSinglePendingAdmission) {
            verifyInitialAdmissionDecision(
                stageDirectory = stageDirectory,
                manifestSha256 = manifestSha256,
                datasetSha256 = actualDatasetSha256
            )
        } else {
            val latestDecision = readLatestAdmissionDecision(
                revisionDirectory = stageDirectory,
                manifestText = manifestText,
                manifest = manifest
            )
            if (latestDecision == UNKNOWN_ADMISSION ||
                latestDecision == LEGACY_UNVERIFIED_ADMISSION
            ) {
                throw IllegalStateException("The committed admission decision log is invalid.")
            }
        }
    }

    private fun hasCanonicalJsonlLineEndings(file: File): Boolean {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var totalBytes = 0L
        var lastByte = -1

        BufferedInputStream(FileInputStream(file), STREAM_BUFFER_SIZE).use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                for (index in 0 until count) {
                    if ((buffer[index].toInt() and 0xff) == CARRIAGE_RETURN_BYTE_VALUE) {
                        return false
                    }
                }
                totalBytes += count.toLong()
                lastByte = buffer[count - 1].toInt() and 0xff
            }
        }
        return totalBytes > 0L && lastByte == NEW_LINE_BYTE_VALUE
    }

    private fun canonicalRecordsFileSha256(recordsFile: File): String {
        val digest = CanonicalDatasetDigests.newDatasetDigest()
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var totalBytes = 0L
        var lastByte = -1

        BufferedInputStream(
            FileInputStream(recordsFile),
            STREAM_BUFFER_SIZE
        ).use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                digest.update(buffer, 0, count)
                totalBytes += count.toLong()
                lastByte = buffer[count - 1].toInt() and 0xff
            }
        }

        if (totalBytes == 0L || lastByte != NEW_LINE_BYTE_VALUE) {
            throw IllegalStateException("The canonical records file is empty or lacks its LF terminator.")
        }
        return CanonicalDatasetDigests.finish(digest)
    }

    private fun verifyInitialAdmissionDecision(
        stageDirectory: File,
        manifestSha256: String,
        datasetSha256: String
    ) {
        val admissionFile = File(stageDirectory, ADMISSION_DECISIONS_FILE)
        if (!admissionFile.isFile || !hasCanonicalJsonlLineEndings(admissionFile)) {
            throw IllegalStateException("The staged dataset admission decision is missing or malformed.")
        }
        admissionFile.bufferedReader(Charsets.UTF_8).use { reader ->
            val firstLine = reader.readLine()
                ?: throw IllegalStateException("The staged dataset admission log is empty.")
            val decision = try {
                JSONObject(firstLine)
            } catch (exception: Exception) {
                throw IllegalStateException("The staged dataset admission record is malformed.", exception)
            }
            val reasonCodes = decision.optJSONArray("reasonCodes")
            val checks = decision.optJSONObject("checks")
            if (reader.readLine() != null ||
                decision.optInt("schemaVersion", -1) != ADMISSION_LOG_SCHEMA_VERSION ||
                reasonCodes == null || reasonCodes.length() == 0 ||
                checks == null ||
                decision.optString("validatorVersion") != ADMISSION_VALIDATOR_VERSION ||
                decision.optString("policyVersion") != ADMISSION_POLICY_VERSION ||
                decision.optString("decision") != "PENDING" ||
                decision.optString("datasetManifestSha256") != manifestSha256 ||
                decision.optString("datasetSha256") != datasetSha256 ||
                !CanonicalDatasetDigests.isSha256Hex(manifestSha256) ||
                !CanonicalDatasetDigests.isSha256Hex(datasetSha256) ||
                !isValidId(decision.optString("decisionId"))
            ) {
                throw IllegalStateException("The staged dataset admission decision is inconsistent.")
            }
        }
    }

    private fun writeInitialAdmissionDecision(
        stageDirectory: File,
        manifestSha256: String,
        datasetSha256: String
    ) {
        val admissionFile = File(stageDirectory, ADMISSION_DECISIONS_FILE)
        if (!admissionFile.createNewFile()) {
            throw IllegalStateException("The dataset admission log already exists.")
        }

        val decision = JSONObject()
            .put("schemaVersion", ADMISSION_LOG_SCHEMA_VERSION)
            .put("decisionId", UUID.randomUUID().toString())
            .put("datasetManifestSha256", manifestSha256)
            .put("datasetSha256", datasetSha256)
            .put("validatorVersion", ADMISSION_VALIDATOR_VERSION)
            .put("policyVersion", ADMISSION_POLICY_VERSION)
            .put("decision", "PENDING")
            .put(
                "reasonCodes",
                JSONArray(
                    listOf(
                        "PROVENANCE_AND_EMBEDDED_MATERIAL_REVIEW_REQUIRED",
                        "TOKENIZER_COMPATIBILITY_NOT_ESTABLISHED",
                        "DATA_QUALITY_POLICY_NOT_EVALUATED",
                        "SEMANTIC_SPLIT_LEAKAGE_NOT_EVALUATED"
                    )
                )
            )
            .put(
                "checks",
                JSONObject()
                    .put("sourceArtifactDigestFormat", "VERIFIED")
                    .put("canonicalDatasetDigest", "VERIFIED")
                    .put("splitMembershipDigests", "VERIFIED")
                    .put("sourceDocumentGrouping", "VERIFIED")
                    .put("provenanceAndEmbeddedMaterialReview", "PENDING")
                    .put("tokenizerCompatibility", "PENDING")
                    .put("dataQuality", "PENDING")
                    .put("semanticLeakage", "PENDING")
            )
            .put("actor", "LOCAL_IMPORTER")
            .put("createdAtEpochMillis", System.currentTimeMillis())

        FileOutputStream(admissionFile, true).bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(decision.toString())
            writer.write("\n")
            writer.flush()
        }
    }

    private fun readLatestAdmissionDecision(
        revisionDirectory: File,
        manifestText: String,
        manifest: JSONObject
    ): String {
        if (manifest.optInt("schemaVersion", -1) != MANIFEST_SCHEMA_VERSION) {
            return LEGACY_UNVERIFIED_ADMISSION
        }

        val manifestDigestFile = File(revisionDirectory, MANIFEST_DIGEST_FILE)
        if (!manifestDigestFile.isFile) return UNKNOWN_ADMISSION

        val manifestSha256 = CanonicalDatasetDigests.sha256Hex(
            manifestText.toByteArray(StandardCharsets.UTF_8)
        )
        if (manifestDigestFile.readText(Charsets.UTF_8).trim() != manifestSha256) {
            return UNKNOWN_ADMISSION
        }

        val datasetIdentity = manifest.optJSONObject("datasetIdentity")
            ?: return UNKNOWN_ADMISSION
        val datasetSha256 = datasetIdentity.optString("canonicalSha256")
        if (!CanonicalDatasetDigests.isSha256Hex(datasetSha256)) {
            return UNKNOWN_ADMISSION
        }

        val logDescriptor = manifest.optJSONObject("admissionDecisionLog")
            ?: return UNKNOWN_ADMISSION
        if (logDescriptor.optString("format") != "jsonl" ||
            logDescriptor.optString("path") != ADMISSION_DECISIONS_FILE ||
            !logDescriptor.optBoolean("appendOnly", false)
        ) {
            return UNKNOWN_ADMISSION
        }

        val admissionFile = File(revisionDirectory, ADMISSION_DECISIONS_FILE)
        if (!admissionFile.isFile) return UNKNOWN_ADMISSION

        if (!hasCanonicalJsonlLineEndings(admissionFile)) {
            return UNKNOWN_ADMISSION
        }

        val seenDecisionIds = HashSet<String>()
        var latestDecision: String? = null
        var decisionCount = 0
        var valid = true

        admissionFile.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) {
                    valid = false
                    break
                }
                val decision = try {
                    JSONObject(line)
                } catch (_: Exception) {
                    valid = false
                    break
                }
                val decisionId = decision.optString("decisionId")
                val decisionValue = decision.optString("decision")
                if (decision.optInt("schemaVersion", -1) != ADMISSION_LOG_SCHEMA_VERSION ||
                    !isValidId(decisionId) ||
                    !seenDecisionIds.add(decisionId) ||
                    decision.optString("datasetManifestSha256") != manifestSha256 ||
                    decision.optString("datasetSha256") != datasetSha256 ||
                    decision.optString("validatorVersion").isBlank() ||
                    decision.optString("policyVersion").isBlank() ||
                    decision.optString("actor").isBlank() ||
                    decision.optLong("createdAtEpochMillis", 0L) <= 0L ||
                    decisionValue !in ADMISSION_DECISIONS
                ) {
                    valid = false
                    break
                }
                if (decisionCount == 0) {
                    val reasonCodes = decision.optJSONArray("reasonCodes")
                    val checks = decision.optJSONObject("checks")
                    val expectedReasonCodes = setOf(
                        "PROVENANCE_AND_EMBEDDED_MATERIAL_REVIEW_REQUIRED",
                        "TOKENIZER_COMPATIBILITY_NOT_ESTABLISHED",
                        "DATA_QUALITY_POLICY_NOT_EVALUATED",
                        "SEMANTIC_SPLIT_LEAKAGE_NOT_EVALUATED"
                    )
                    val actualReasonCodes = if (reasonCodes == null) {
                        emptySet()
                    } else {
                        buildSet {
                            for (index in 0 until reasonCodes.length()) {
                                add(reasonCodes.optString(index))
                            }
                        }
                    }
                    if (decisionValue != INITIAL_ADMISSION_DECISION ||
                        decision.optString("validatorVersion") != ADMISSION_VALIDATOR_VERSION ||
                        decision.optString("policyVersion") != ADMISSION_POLICY_VERSION ||
                        reasonCodes == null ||
                        reasonCodes.length() != expectedReasonCodes.size ||
                        actualReasonCodes != expectedReasonCodes ||
                        checks == null ||
                        checks.optString("sourceArtifactDigestFormat") != "VERIFIED" ||
                        checks.optString("canonicalDatasetDigest") != "VERIFIED" ||
                        checks.optString("splitMembershipDigests") != "VERIFIED" ||
                        checks.optString("sourceDocumentGrouping") != "VERIFIED" ||
                        checks.optString("provenanceAndEmbeddedMaterialReview") != "PENDING" ||
                        checks.optString("tokenizerCompatibility") != "PENDING" ||
                        checks.optString("dataQuality") != "PENDING" ||
                        checks.optString("semanticLeakage") != "PENDING"
                    ) {
                        valid = false
                        break
                    }
                }
                latestDecision = decisionValue
                decisionCount += 1
            }
        }

        return if (valid && decisionCount > 0) {
            latestDecision ?: UNKNOWN_ADMISSION
        } else {
            UNKNOWN_ADMISSION
        }
    }

    private fun summaryFromManifest(
        manifest: JSONObject,
        lastAdmissionDecision: String
    ): LocalCorpusSummary {
        val counts = manifest.optJSONObject("counts") ?: JSONObject()
        val purposeArray = manifest.optJSONArray("purposes") ?: JSONArray()
        val purposes = buildSet {
            for (index in 0 until purposeArray.length()) {
                val value = purposeArray.optString(index)
                DatasetPurpose.entries.firstOrNull { it.manifestValue == value }?.let(::add)
            }
        }
        return LocalCorpusSummary(
            corpusId = manifest.optString("corpusId"),
            revision = manifest.optInt("revision", 1),
            importedAtEpochMillis = manifest.optLong("importedAtEpochMillis"),
            recordCount = counts.optLong("acceptedRecords"),
            trainRecordCount = counts.optLong("trainRecords"),
            validationRecordCount = counts.optLong("validationRecords"),
            testRecordCount = counts.optLong("testRecords"),
            purposes = purposes,
            lastAdmissionDecision = lastAdmissionDecision
        )
    }

    private fun queryDisplayName(uri: Uri): String {
        val cursor = appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        val displayName = cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else {
                null
            }
        }
        return displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "selected-file"
    }

    private fun isZip(displayName: String, mimeType: String?): Boolean {
        return extensionOf(displayName) == "zip" ||
            mimeType == "application/zip" ||
            mimeType == "application/x-zip-compressed"
    }

    private fun isSupportedTextFile(name: String): Boolean {
        return extensionOf(name) in SUPPORTED_TEXT_EXTENSIONS
    }

    private fun extensionOf(name: String): String {
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }

    private fun mimeTypeForName(name: String): String {
        return when (extensionOf(name)) {
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "jsonl", "ndjson" -> "application/x-ndjson"
            else -> "application/octet-stream"
        }
    }

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.startsWith('/') || path.startsWith('\\')) {
            return false
        }
        val segments = path.split('/', '\\')
        return segments.none { it == ".." || it == "." }
    }

    private suspend fun drainInput(input: InputStream) {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            if (input.read(buffer) < 0) break
        }
    }

    private suspend fun drainZipEntry(input: ZipInputStream) {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            if (input.read(buffer) < 0) break
        }
    }

    private fun normalizeText(source: String): String {
        val nfc = Normalizer.normalize(source, Normalizer.Form.NFC)
        return nfc
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .joinToString("\n") { it.trimEnd() }
            .trim()
    }

    private fun sha256Text(source: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
        return digestToHex(digest)
    }

    private fun digestToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { byte ->
            String.format(Locale.US, "%02x", byte.toInt() and 0xff)
        }
    }

    private fun addWarning(warnings: MutableList<String>, warning: String) {
        if (warnings.size < MAX_WARNINGS) {
            warnings.add(warning)
        }
    }

    private fun isValidId(value: String): Boolean {
        return value.matches(Regex("^[0-9a-fA-F-]{36}$"))
    }

    private data class ImportSource(
        val displayName: String,
        val mimeType: String?,
        val openStream: () -> InputStream?
    )

    private data class SourceInfo(
        val sizeBytes: Long,
        val sha256: String
    )

    private data class ParsedFile(
        val file: File?,
        val isValid: Boolean,
        val rejectedRecords: Long
    )

    private data class DocumentMetadata(
        val digest: String,
        val sourceArtifactSha256: String,
        val displayName: String,
        val sizeBytes: Long,
        val mediaType: String?,
        var recordCount: Long
    )

    private data class IngestResult(
        val acceptedRecords: Long,
        val duplicateRecords: Long,
        val rejectedRecords: Long,
        val documentAccepted: Boolean
    )

    private data class ArchiveResult(
        val entryCount: Int,
        val acceptedRecords: Long,
        val duplicateRecords: Long,
        val rejectedRecords: Long,
        val skippedFiles: Int,
        val importedBytes: Long
    )

    private data class SplitCounts(
        val train: Long,
        val validation: Long,
        val test: Long
    ) {
        val total: Long
            get() = train + validation + test
    }

    private data class FinalRecordMetadata(
        val counts: SplitCounts,
        val canonicalDatasetSha256: String,
        val memberships: Map<String, DatasetSplitMembership>,
        val splitManifestSha256: String
    )

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var bytesRead: Long = 0L
            private set

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) {
                bytesRead += 1L
            }
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) {
                bytesRead += count.toLong()
            }
            return count
        }
    }

    private class StorageGuard(private val directory: File) {
        private var bytesSinceLastCheck = 0L

        fun beforeWrite(estimatedBytes: Long) {
            bytesSinceLastCheck += estimatedBytes
            if (bytesSinceLastCheck >= STORAGE_CHECK_INTERVAL_BYTES) {
                if (directory.usableSpace <= 0L) {
                    throw IOException("The device has no free storage left for the local corpus import.")
                }
                bytesSinceLastCheck = 0L
            }
        }
    }

    companion object {
        private const val BUNDLED_ARGUS_ID_ASSET = "corpus/ArgusID.txt"
        private const val BUNDLED_DIGITRANSID_ASSET = "corpus/DigiTransID.zip"
        private const val STAGING_DIRECTORY = "corpus-staging"
        private const val CORPORA_DIRECTORY = "corpora"
        private const val MANIFEST_FILE = "manifest.json"
        private const val MANIFEST_DIGEST_FILE = "manifest.sha256"
        private const val ADMISSION_DECISIONS_FILE = "admission-decisions.jsonl"
        private const val ADMISSION_LOG_SCHEMA_VERSION = 1
        private const val ADMISSION_VALIDATOR_VERSION = "local-import-integrity-v1"
        private const val ADMISSION_POLICY_VERSION = "dataset-admission-pending-v1"
        private const val INITIAL_ADMISSION_DECISION = "PENDING"
        private const val LEGACY_UNVERIFIED_ADMISSION = "LEGACY_UNVERIFIED"
        private const val UNKNOWN_ADMISSION = "UNKNOWN"
        private val ADMISSION_DECISIONS = setOf("PENDING", "APPROVED", "REJECTED")
        private const val PARSER_VERSION = "argus-document-parser-v1"
        private const val NORMALIZATION_VERSION = "argus-unicode-nfc-lf-trim-v1"
        private const val DEDUPLICATION_VERSION = "sha256-normalized-record-text-v1"
        private const val RAW_RECORDS_FILE = "raw-records.jsonl"
        private const val RECORDS_FILE = "records.jsonl"
        private const val DEDUPLICATION_DATABASE_FILE = "record-deduplication.db"
        private const val EXCLUDED_DUPLICATE_ONLY_SPLIT = "EXCLUDED_DUPLICATE_ONLY"
        private const val IMPORTER_VERSION = "3.0.0"
        private const val MANIFEST_SCHEMA_VERSION = 3
        private const val SPLIT_ALGORITHM = "sha256-source-document-v1"
        private const val SPLIT_SEED = "argus-local-corpus-split-v1"
        private const val VALIDATION_RATIO = 0.10
        private const val TEST_RATIO = 0.10
        private const val MAX_SELECTED_SOURCES = 64
        private const val MAX_ARCHIVE_ENTRIES = 4096
        private const val MAX_WARNINGS = 100
        private const val MAX_TEXT_RECORD_CHARACTERS = 262_144
        private const val MAX_JSONL_LINE_CHARACTERS = 1_048_576
        private const val STREAM_BUFFER_SIZE = 64 * 1024
        private const val NEW_LINE_BYTE_VALUE = 0x0a
        private const val CARRIAGE_RETURN_BYTE_VALUE = 0x0d
        private const val CANCELLATION_CHECK_RECORD_INTERVAL = 256
        private const val STORAGE_CHECK_INTERVAL_BYTES = 4L * 1024L * 1024L
        private val SUPPORTED_TEXT_EXTENSIONS = setOf("txt", "md", "jsonl", "ndjson")
    }
}
