package com.argus.agi.platform.android.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import android.provider.OpenableColumns
import com.argus.agi.core.training.CanonicalDatasetDigests
import com.argus.agi.core.training.CorpusImportPreview
import com.argus.agi.core.training.CorpusImportStager
import com.argus.agi.core.training.CorpusManifestContract
import com.argus.agi.core.training.CorpusStorageLayout
import com.argus.agi.core.training.CorpusTextNormalizer
import com.argus.agi.core.training.DatasetPurpose
import com.argus.agi.core.training.DatasetSplitMembership
import com.argus.agi.core.training.LocalCorpusStore
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
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

/**
 * Streams local sources into app-private staging. Text is decoded incrementally,
 * ZIP entries are processed directly from ZipInputStream, and deduplication is
 * disk-backed. No source file or archive entry is loaded as a whole byte array.
 */
class LocalDatasetImporter(
    context: Context,
    private val corpusStore: LocalCorpusStore
) : CorpusImportStager {

    private val appContext: Context = context.applicationContext

    private suspend fun stageUris(
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

    override suspend fun stageBundledArgusId(
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
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

    override suspend fun stageBundledDigiTransId(
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
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

    override suspend fun stageUserFiles(
        sourceUriReferences: List<String>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        return stageUris(sourceUriReferences.map(Uri::parse), purposes)
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
        val stagingRoot = File(appContext.filesDir, CorpusStorageLayout.STAGING_DIRECTORY)
        if (!stagingRoot.exists() && !stagingRoot.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }
        val stageDirectory = File(stagingRoot, stageId)
        if (!stageDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }

        try {
            return stageInto(sources, purposes, stageId, corpusId, stageDirectory)
        } catch (exception: Exception) {
            try {
                corpusStore.discard(stageId)
            } catch (cleanupException: Exception) {
                exception.addSuppressed(cleanupException)
            }
            throw exception
        }
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
            .put("grouping", SPLIT_GROUPING)
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

        val manifestSha256 = corpusStore.writeStagedManifest(
            stageId = stageId,
            manifestJson = manifest.toString(2)
        )
        corpusStore.writeInitialAdmissionDecision(
            stageId = stageId,
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
        val normalized = CorpusTextNormalizer.normalize(source)
        if (normalized.isBlank()) return

        val serializedRecord = JSONObject().put("text", normalized).toString()
        storageGuard.beforeWrite(serializedRecord.length.toLong() * 3L + 2L)
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

                val recordDigest = CorpusTextNormalizer.sha256(text)
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
                storageGuard.beforeWrite(serializedRecord.length.toLong() * 3L + 2L)
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
            CorpusTextNormalizer.sha256(SPLIT_SEED + ":" + documentId)
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
            BufferedOutputStream(
                FileOutputStream(finalRecordsFile),
                STREAM_BUFFER_SIZE
            ).use { writer ->
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

                    val canonicalBytes = canonicalLine.toByteArray(StandardCharsets.UTF_8)
                    storageGuard.beforeWrite(canonicalBytes.size.toLong() + 1L)
                    writer.write(canonicalBytes)
                    writer.write(NEW_LINE_BYTE_VALUE)

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
            grouping = SPLIT_GROUPING,
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
        private const val RAW_RECORDS_FILE = "raw-records.jsonl"
        private const val DEDUPLICATION_DATABASE_FILE = "record-deduplication.db"

        private const val ADMISSION_DECISIONS_FILE = CorpusStorageLayout.ADMISSION_DECISIONS_FILE
        private const val RECORDS_FILE = CorpusStorageLayout.RECORDS_FILE
        private const val STREAM_BUFFER_SIZE = CorpusStorageLayout.STREAM_BUFFER_SIZE
        private const val NEW_LINE_BYTE_VALUE = CorpusStorageLayout.NEW_LINE_BYTE_VALUE

        private const val PARSER_VERSION = CorpusManifestContract.PARSER_VERSION
        private const val NORMALIZATION_VERSION = CorpusManifestContract.NORMALIZATION_VERSION
        private const val DEDUPLICATION_VERSION = CorpusManifestContract.DEDUPLICATION_VERSION
        private const val EXCLUDED_DUPLICATE_ONLY_SPLIT =
            CorpusManifestContract.EXCLUDED_DUPLICATE_ONLY_SPLIT
        private const val IMPORTER_VERSION = CorpusManifestContract.IMPORTER_VERSION
        private const val MANIFEST_SCHEMA_VERSION = CorpusManifestContract.MANIFEST_SCHEMA_VERSION
        private const val SPLIT_ALGORITHM = CorpusManifestContract.SPLIT_ALGORITHM
        private const val SPLIT_SEED = CorpusManifestContract.SPLIT_SEED
        private const val SPLIT_GROUPING = CorpusManifestContract.SPLIT_GROUPING
        private const val VALIDATION_RATIO = CorpusManifestContract.VALIDATION_RATIO
        private const val TEST_RATIO = CorpusManifestContract.TEST_RATIO

        private const val MAX_SELECTED_SOURCES = 64
        private const val MAX_ARCHIVE_ENTRIES = 4096
        private const val MAX_WARNINGS = 100
        private const val MAX_TEXT_RECORD_CHARACTERS = 262_144
        private const val MAX_JSONL_LINE_CHARACTERS = 1_048_576
        private const val CANCELLATION_CHECK_RECORD_INTERVAL = 256
        private const val STORAGE_CHECK_INTERVAL_BYTES = 4L * 1024L * 1024L
        private val SUPPORTED_TEXT_EXTENSIONS = setOf("txt", "md", "jsonl", "ndjson")
    }
