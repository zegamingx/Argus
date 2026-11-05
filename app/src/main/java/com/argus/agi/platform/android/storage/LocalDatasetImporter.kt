package com.argus.agi.platform.android.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
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
    val acceptedRecordCount: Int,
    val duplicateRecordCount: Int,
    val rejectedRecordCount: Int,
    val skippedFileCount: Int,
    val importedBytes: Long,
    val trainRecordCount: Int,
    val validationRecordCount: Int,
    val testRecordCount: Int,
    val warnings: List<String>
)

data class LocalCorpusSummary(
    val corpusId: String,
    val revision: Int,
    val importedAtEpochMillis: Long,
    val recordCount: Int,
    val trainRecordCount: Int,
    val validationRecordCount: Int,
    val testRecordCount: Int,
    val purposes: Set<DatasetPurpose>
)

/**
 * Imports user-selected general-corpus files into app-private storage.
 *
 * Supported inputs are UTF-8 TXT, Markdown, JSONL and NDJSON files, either
 * selected individually or contained in a ZIP archive. Archive entries are
 * read as streams and are never extracted using archive-provided paths.
 */
class LocalDatasetImporter(context: Context) {

    private val appContext: Context = context.applicationContext

    fun stage(
        uris: List<Uri>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        val sources = uris.map { uri ->
            ImportSource(
                displayName = queryDisplayName(uri),
                mimeType = appContext.contentResolver.getType(uri),
                openStream = { appContext.contentResolver.openInputStream(uri) }
            )
        }
        return stageSources(sources, purposes)
    }

    fun stageBundledDigiTransId(purposes: Set<DatasetPurpose>): CorpusImportPreview {
        val source = ImportSource(
            displayName = "DigiTransID.zip",
            mimeType = "application/zip",
            openStream = { appContext.assets.open(BUNDLED_DIGITRANSID_ASSET) }
        )
        return stageSources(listOf(source), purposes)
    }

    private fun stageSources(
        sources: List<ImportSource>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        require(sources.isNotEmpty()) { "Select at least one local dataset file." }
        require(purposes.isNotEmpty()) { "Select at least one dataset purpose." }
        require(sources.size <= MAX_SELECTED_SOURCES) {
            "The import accepts at most $MAX_SELECTED_SOURCES selected files at once."
        }

        val stageId = UUID.randomUUID().toString()
        val corpusId = UUID.randomUUID().toString()
        val stageDirectory = File(File(appContext.filesDir, STAGING_DIRECTORY), stageId)
        if (!stageDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }

        try {
            return stageInto(
                sources = sources,
                purposes = purposes,
                stageId = stageId,
                corpusId = corpusId,
                stageDirectory = stageDirectory
            )
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

        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val corpusId = manifest.optString("corpusId")
        if (!isValidId(corpusId)) {
            throw IllegalStateException("The staged corpus manifest is invalid.")
        }

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

        return summaryFromManifest(manifest)
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
                        summaryFromManifest(JSONObject(manifestFile.readText(Charsets.UTF_8)))
                    }.getOrNull()
                }
            }
            .sortedByDescending { it.importedAtEpochMillis }
            .toList()
    }

    private fun stageInto(
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
        val seenRecordDigests = HashSet<String>()
        val warnings = mutableListOf<String>()
        var acceptedRecords = 0
        var duplicateRecords = 0
        var rejectedRecords = 0
        var skippedFiles = 0
        var importedBytes = 0L
        var expandedArchiveBytes = 0L
        var archiveEntryCount = 0

        BufferedWriter(rawRecordsFile.writer(Charsets.UTF_8)).use { writer ->
            sources.forEach { source ->
                val sourceName = source.displayName
                val mimeType = source.mimeType
                val archive = isZip(sourceName, mimeType)
                val sourceBytes = source.openStream()?.use { input ->
                    readBounded(input, if (archive) MAX_ARCHIVE_BYTES else MAX_DOCUMENT_BYTES)
                } ?: throw IllegalStateException("Could not open the selected file: $sourceName")

                val sourceDigest = sha256(sourceBytes)
                sourceMetadata.put(
                    JSONObject()
                        .put("name", sourceName)
                        .put("mimeType", mimeType ?: JSONObject.NULL)
                        .put("sizeBytes", sourceBytes.size)
                        .put("sha256", sourceDigest)
                        .put("archive", archive)
                )

                if (archive) {
                    ZipInputStream(ByteArrayInputStream(sourceBytes)).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            archiveEntryCount += 1
                            if (archiveEntryCount > MAX_ARCHIVE_ENTRIES) {
                                throw IllegalArgumentException(
                                    "The selected ZIP files contain more than $MAX_ARCHIVE_ENTRIES entries."
                                )
                            }

                            if (entry.isDirectory) {
                                zip.closeEntry()
                                continue
                            }

                            val entryName = entry.name
                            val remainingExpandedBytes = MAX_TOTAL_EXPANDED_BYTES - expandedArchiveBytes
                            if (remainingExpandedBytes <= 0L) {
                                throw IllegalArgumentException("The ZIP contents exceed the local import size limit.")
                            }
                            val entryBytes = readBounded(
                                input = zip,
                                maxBytes = minOf(MAX_DOCUMENT_BYTES, remainingExpandedBytes)
                            )
                            expandedArchiveBytes += entryBytes.size

                            if (!isSafeArchivePath(entryName)) {
                                skippedFiles += 1
                                addWarning(warnings, "Skipped an unsafe archive path.")
                                zip.closeEntry()
                                continue
                            }

                            if (!isSupportedTextFile(entryName)) {
                                skippedFiles += 1
                                addWarning(warnings, "Skipped unsupported archive entry: $entryName")
                                zip.closeEntry()
                                continue
                            }

                            importedBytes += entryBytes.size
                            val result = processTextDocument(
                                displayName = "$sourceName!/$entryName",
                                mediaType = mimeTypeForName(entryName),
                                bytes = entryBytes,
                                writer = writer,
                                seenRecordDigests = seenRecordDigests,
                                documentMetadata = documentMetadata,
                                warnings = warnings
                            )
                            acceptedRecords += result.acceptedRecords
                            duplicateRecords += result.duplicateRecords
                            rejectedRecords += result.rejectedRecords
                            if (!result.documentAccepted) {
                                skippedFiles += 1
                            }
                            zip.closeEntry()
                        }
                    }
                } else if (isSupportedTextFile(sourceName)) {
                    importedBytes += sourceBytes.size
                    if (importedBytes > MAX_TOTAL_EXPANDED_BYTES) {
                        throw IllegalArgumentException("The selected files exceed the local import size limit.")
                    }
                    val result = processTextDocument(
                        displayName = sourceName,
                        mediaType = mimeType,
                        bytes = sourceBytes,
                        writer = writer,
                        seenRecordDigests = seenRecordDigests,
                        documentMetadata = documentMetadata,
                        warnings = warnings
                    )
                    acceptedRecords += result.acceptedRecords
                    duplicateRecords += result.duplicateRecords
                    rejectedRecords += result.rejectedRecords
                    if (!result.documentAccepted) {
                        skippedFiles += 1
                    }
                } else {
                    skippedFiles += 1
                    addWarning(
                        warnings,
                        "Skipped $sourceName because only TXT, Markdown, JSONL, NDJSON and ZIP are supported."
                    )
                }
            }
        }

        if (acceptedRecords == 0 || documentMetadata.isEmpty()) {
            throw IllegalArgumentException(
                "No usable text records were found. Supported formats are TXT, Markdown, JSONL, NDJSON and ZIP."
            )
        }

        val splitByDocument = createDocumentSplits(documentMetadata.keys.toList(), warnings)
        val splitCounts = writeFinalRecords(
            rawRecordsFile = rawRecordsFile,
            finalRecordsFile = finalRecordsFile,
            splitByDocument = splitByDocument
        )
        rawRecordsFile.delete()

        val documentsJson = JSONArray()
        documentMetadata.values.forEach { document ->
            documentsJson.put(
                JSONObject()
                    .put("name", document.displayName)
                    .put("sha256", document.digest)
                    .put("sizeBytes", document.sizeBytes)
                    .put("mediaType", document.mediaType ?: JSONObject.NULL)
                    .put("recordCount", document.recordCount)
                    .put("split", splitByDocument[document.digest] ?: "train")
            )
        }

        val manifest = JSONObject()
            .put("schemaVersion", MANIFEST_SCHEMA_VERSION)
            .put("corpusId", corpusId)
            .put("revision", 1)
            .put("importedAtEpochMillis", System.currentTimeMillis())
            .put("importerVersion", IMPORTER_VERSION)
            .put("recordFormat", "jsonl")
            .put("purposes", JSONArray(purposes.map { it.manifestValue }.sorted()))
            .put("sources", sourceMetadata)
            .put("documents", documentsJson)
            .put(
                "counts",
                JSONObject()
                    .put("acceptedRecords", acceptedRecords)
                    .put("duplicateRecords", duplicateRecords)
                    .put("rejectedRecords", rejectedRecords)
                    .put("skippedFiles", skippedFiles)
                    .put("importedBytes", importedBytes)
                    .put("trainRecords", splitCounts.train)
                    .put("validationRecords", splitCounts.validation)
                    .put("testRecords", splitCounts.test)
            )
            .put(
                "split",
                JSONObject()
                    .put("algorithm", SPLIT_ALGORITHM)
                    .put("seed", SPLIT_SEED)
                    .put("grouping", "source-document")
            )
            .put("warnings", JSONArray(warnings))

        File(stageDirectory, MANIFEST_FILE).writeText(manifest.toString(2), Charsets.UTF_8)

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
            trainRecordCount = splitCounts.train,
            validationRecordCount = splitCounts.validation,
            testRecordCount = splitCounts.test,
            warnings = warnings.toList()
        )
    }

    private fun processTextDocument(
        displayName: String,
        mediaType: String?,
        bytes: ByteArray,
        writer: BufferedWriter,
        seenRecordDigests: MutableSet<String>,
        documentMetadata: MutableMap<String, DocumentMetadata>,
        warnings: MutableList<String>
    ): DocumentResult {
        val digest = sha256(bytes)
        val decodedText = decodeUtf8(bytes)
        if (decodedText == null) {
            addWarning(warnings, "Skipped $displayName because it is not valid UTF-8.")
            return DocumentResult(0, 0, 1, false)
        }

        val extension = extensionOf(displayName)
        val parsedRecords = when (extension) {
            "txt", "md" -> ParsedRecords(splitTextDocument(decodedText), 0)
            "jsonl", "ndjson" -> parseJsonLines(decodedText, displayName, warnings)
            else -> ParsedRecords(emptyList(), 1)
        }
        val candidates = parsedRecords.records

        var accepted = 0
        var duplicates = 0
        var rejected = parsedRecords.rejectedRecords
        val normalizedCandidates = candidates.mapNotNull { candidate ->
            val normalized = normalizeText(candidate)
            if (normalized.isBlank()) {
                rejected += 1
                null
            } else {
                normalized
            }
        }
        rejected += candidates.size - normalizedCandidates.size

        normalizedCandidates.forEach { text ->
            val recordDigest = sha256(text.toByteArray(Charsets.UTF_8))
            if (recordDigest in seenRecordDigests) {
                duplicates += 1
            } else {
                if (seenRecordDigests.size >= MAX_UNIQUE_RECORDS) {
                    throw IllegalArgumentException(
                        "The corpus exceeds the current limit of $MAX_UNIQUE_RECORDS unique records."
                    )
                }
                seenRecordDigests.add(recordDigest)
                val record = JSONObject()
                    .put("recordId", recordDigest)
                    .put("sourceDocumentId", digest)
                    .put("sourceName", displayName)
                    .put("text", text)
                writer.write(record.toString())
                writer.newLine()
                accepted += 1
            }
        }

        if (accepted == 0 && duplicates == 0) {
            addWarning(warnings, "No usable records were found in $displayName.")
        }

        val existing = documentMetadata[digest]
        if (existing == null && (accepted > 0 || duplicates > 0)) {
            documentMetadata[digest] = DocumentMetadata(
                digest = digest,
                displayName = displayName,
                sizeBytes = bytes.size.toLong(),
                mediaType = mediaType,
                recordCount = accepted
            )
        } else if (existing != null) {
            existing.recordCount += accepted
        }

        return DocumentResult(
            acceptedRecords = accepted,
            duplicateRecords = duplicates,
            rejectedRecords = rejected,
            documentAccepted = accepted > 0 || duplicates > 0
        )
    }

    private fun parseJsonLines(
        source: String,
        displayName: String,
        warnings: MutableList<String>
    ): ParsedRecords {
        val records = mutableListOf<String>()
        var invalidLines = 0
        source.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEach
            }
            val extracted = runCatching {
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
                        }.distinct().joinToString("\n")
                    }
                    is String -> value
                    else -> ""
                }
            }.getOrNull()

            if (extracted.isNullOrBlank()) {
                invalidLines += 1
            } else {
                records.add(extracted)
            }
        }
        if (invalidLines > 0) {
            addWarning(warnings, "$displayName contains $invalidLines empty or unsupported JSONL rows.")
        }
        return ParsedRecords(records, invalidLines)
    }

    private fun splitTextDocument(source: String): List<String> {
        val normalizedLineEndings = source.replace("\r\n", "\n").replace('\r', '\n')
        return normalizedLineEndings
            .split(Regex("\n[\\t ]*\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
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

    private fun createDocumentSplits(
        documentIds: List<String>,
        warnings: MutableList<String>
    ): Map<String, String> {
        val ordered = documentIds.sortedBy { documentId ->
            sha256("$SPLIT_SEED:$documentId".toByteArray(Charsets.UTF_8))
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

    private fun writeFinalRecords(
        rawRecordsFile: File,
        finalRecordsFile: File,
        splitByDocument: Map<String, String>
    ): SplitCounts {
        var train = 0
        var validation = 0
        var test = 0
        rawRecordsFile.bufferedReader(Charsets.UTF_8).use { reader ->
            finalRecordsFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) {
                        continue
                    }
                    val record = JSONObject(line)
                    val documentId = record.optString("sourceDocumentId")
                    val split = splitByDocument[documentId] ?: "train"
                    record.put("split", split)
                    writer.write(record.toString())
                    writer.newLine()
                    when (split) {
                        "validation" -> validation += 1
                        "test" -> test += 1
                        else -> train += 1
                    }
                }
            }
        }
        return SplitCounts(train, validation, test)
    }

    private fun summaryFromManifest(manifest: JSONObject): LocalCorpusSummary {
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
            recordCount = counts.optInt("acceptedRecords"),
            trainRecordCount = counts.optInt("trainRecords"),
            validationRecordCount = counts.optInt("validationRecords"),
            testRecordCount = counts.optInt("testRecords"),
            purposes = purposes
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

    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val readCount = input.read(buffer)
            if (readCount < 0) {
                break
            }
            totalBytes += readCount
            if (totalBytes > maxBytes) {
                throw IllegalArgumentException(
                    "A selected file or archive entry exceeds the configured local import limit."
                )
            }
            output.write(buffer, 0, readCount)
        }
        return output.toByteArray()
    }

    private fun decodeUtf8(bytes: ByteArray): String? {
        return runCatching {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        }.getOrNull()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte ->
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

    private data class ParsedRecords(
        val records: List<String>,
        val rejectedRecords: Int
    )

    private data class DocumentMetadata(
        val digest: String,
        val displayName: String,
        val sizeBytes: Long,
        val mediaType: String?,
        var recordCount: Int
    )

    private data class DocumentResult(
        val acceptedRecords: Int,
        val duplicateRecords: Int,
        val rejectedRecords: Int,
        val documentAccepted: Boolean
    )

    private data class SplitCounts(
        val train: Int,
        val validation: Int,
        val test: Int
    )

    companion object {
        private const val BUNDLED_DIGITRANSID_ASSET = "corpus/DigiTransID.zip"
        private const val STAGING_DIRECTORY = "corpus-staging"
        private const val CORPORA_DIRECTORY = "corpora"
        private const val MANIFEST_FILE = "manifest.json"
        private const val RAW_RECORDS_FILE = "raw-records.jsonl"
        private const val RECORDS_FILE = "records.jsonl"
        private const val IMPORTER_VERSION = "1.0.0"
        private const val MANIFEST_SCHEMA_VERSION = 1
        private const val SPLIT_ALGORITHM = "sha256-source-document-v1"
        private const val SPLIT_SEED = "argus-local-corpus-split-v1"
        private const val VALIDATION_RATIO = 0.10
        private const val TEST_RATIO = 0.10
        private const val MAX_SELECTED_SOURCES = 64
        private const val MAX_ARCHIVE_ENTRIES = 512
        private const val MAX_UNIQUE_RECORDS = 250_000
        private const val MAX_WARNINGS = 100
        private const val MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L
        private const val MAX_DOCUMENT_BYTES = 32L * 1024L * 1024L
        private const val MAX_TOTAL_EXPANDED_BYTES = 512L * 1024L * 1024L
        private val SUPPORTED_TEXT_EXTENSIONS = setOf("txt", "md", "jsonl", "ndjson")
    }
}
