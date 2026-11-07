package com.argus.agi.data.corpus

import com.argus.agi.core.training.CanonicalDatasetDigests
import com.argus.agi.core.training.DatasetPurpose
import com.argus.agi.core.training.DatasetSplitMembership
import com.argus.agi.core.training.LocalCorpusIntegrityReport
import com.argus.agi.core.training.LocalCorpusSummary
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/**
 * Owns app-private persistence, immutable revision publication, manifest
 * serialization boundaries, admission-log reads and corpus integrity checks.
 *
 * This repository intentionally has no Android Context or UI dependency. The
 * Android importer supplies only the app-private files directory and staged files.
 */
class LocalCorpusRepository(filesDirectory: File) {

    private val appFilesDirectory: File = filesDirectory.canonicalFile

    /**
     * Creates a new staging directory under the private corpus staging root.
     * The returned directory is owned by this repository's storage layout.
     */
    fun createStagingDirectory(stageId: String): File {
        require(isValidId(stageId)) { "The staged corpus identifier is invalid." }
        val stagingRoot = resolveDirectChild(appFilesDirectory, STAGING_DIRECTORY)
        if (!stagingRoot.exists() && !stagingRoot.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }
        val stageDirectory = resolveDirectChild(stagingRoot, stageId)
        if (stageDirectory.exists()) {
            throw IllegalStateException("The local corpus staging identifier already exists.")
        }
        if (!stageDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus staging directory.")
        }
        return stageDirectory
    }

    /**
     * Validates and publishes the first immutable revision of a staged corpus.
     * A successful commit is storage publication only; the admission decision
     * remains PENDING and does not authorize training.
     */
    fun commit(stageId: String): LocalCorpusSummary {
        require(isValidId(stageId)) { "The staged corpus identifier is invalid." }
        val stagingRoot = resolveDirectChild(appFilesDirectory, STAGING_DIRECTORY)
        val stageDirectory = resolveDirectChild(stagingRoot, stageId)
        val manifestFile = File(stageDirectory, MANIFEST_FILE)
        if (!stageDirectory.isDirectory || !manifestFile.isFile) {
            throw IllegalStateException(
                "The staged corpus is no longer available. Import the files again."
            )
        }

        val manifestText = manifestFile.readText(Charsets.UTF_8)
        val manifest = JSONObject(manifestText)
        val corpusId = manifest.optString("corpusId")
        if (!isValidId(corpusId)) {
            throw IllegalStateException("The staged corpus manifest is invalid.")
        }
        if (manifest.optInt("revision", -1) != INITIAL_REVISION) {
            throw IllegalStateException("The staged corpus revision is unsupported.")
        }
        verifyCorpusRevisionContents(stageDirectory, manifestText, manifest)

        val corpusDirectory = resolveDirectChild(
            resolveDirectChild(appFilesDirectory, CORPORA_DIRECTORY),
            corpusId
        )
        val corpusRoot = corpusDirectory.parentFile
            ?: throw IllegalStateException("The local corpus root is invalid.")
        if (!corpusRoot.exists() && !corpusRoot.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus directory.")
        }
        if (!corpusDirectory.exists() && !corpusDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the local corpus directory.")
        }

        val revisionDirectory = resolveDirectChild(
            corpusDirectory,
            revisionDirectoryName(INITIAL_REVISION)
        )
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

        val corporaRoot = resolveDirectChild(appFilesDirectory, CORPORA_DIRECTORY)
        val corpusDirectory = resolveDirectChild(corporaRoot, corpusId)
        val revisionDirectory = resolveDirectChild(
            corpusDirectory,
            revisionDirectoryName(revision)
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
            throw IllegalStateException(
                "The committed corpus identity does not match its storage path."
            )
        }

        verifyCorpusRevisionContents(
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

    /**
     * Removes an uncommitted staging directory. A committed revision is never
     * addressed by this operation.
     */
    fun discard(stageId: String) {
        require(isValidId(stageId)) { "The staged corpus identifier is invalid." }
        val stagingRoot = resolveDirectChild(appFilesDirectory, STAGING_DIRECTORY)
        val stageDirectory = resolveDirectChild(stagingRoot, stageId)
        if (stageDirectory.exists() && !stageDirectory.deleteRecursively()) {
            throw IllegalStateException("Could not discard the staged corpus.")
        }
    }

    /**
     * Returns committed revision summaries without claiming that record content
     * has been fully re-verified. Consumers requiring records must call the
     * explicit integrity verifier before opening the data.
     */
    fun listCommitted(): List<LocalCorpusSummary> {
        val corporaRoot = resolveDirectChild(appFilesDirectory, CORPORA_DIRECTORY)
        val corpusDirectories = corporaRoot.listFiles() ?: return emptyList()
        return corpusDirectories
            .asSequence()
            .filter { directory ->
                directory.isDirectory &&
                    isValidId(directory.name) &&
                    runCatching {
                        directory.canonicalFile.parentFile == corporaRoot.canonicalFile
                    }.getOrDefault(false)
            }
            .flatMap { corpusDirectory ->
                (corpusDirectory.listFiles() ?: emptyArray())
                    .asSequence()
                    .mapNotNull { revisionDirectory ->
                        val revision = parseRevisionDirectoryName(revisionDirectory.name)
                            ?: return@mapNotNull null
                        if (!revisionDirectory.isDirectory) return@mapNotNull null
                        runCatching {
                            val canonicalRevision = resolveDirectChild(
                                corpusDirectory,
                                revisionDirectory.name
                            )
                            val manifestFile = File(canonicalRevision, MANIFEST_FILE)
                            if (!manifestFile.isFile) return@runCatching null
                            val manifestText = manifestFile.readText(Charsets.UTF_8)
                            val manifest = JSONObject(manifestText)
                            if (manifest.optString("corpusId") != corpusDirectory.name ||
                                manifest.optInt("revision", -1) != revision
                            ) {
                                return@runCatching null
                            }
                            val admissionDecision = readLatestAdmissionDecision(
                                revisionDirectory = canonicalRevision,
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

    private fun resolveDirectChild(parent: File, childName: String): File {
        require(childName.isNotBlank() && childName != "." && childName != "..")
        require(!childName.contains('/') && !childName.contains('\\')) {
            "The corpus storage path contains an invalid segment."
        }
        val canonicalParent = parent.canonicalFile
        val child = File(canonicalParent, childName).canonicalFile
        if (child.parentFile != canonicalParent) {
            throw SecurityException("The corpus storage path escapes its owning directory.")
        }
        return child
    }

    private fun revisionDirectoryName(revision: Int): String {
        require(revision > 0) { "The corpus revision must be positive." }
        return "revision-" + revision.toString().padStart(6, '0')
    }

    private fun parseRevisionDirectoryName(name: String): Int? {
        if (!name.startsWith(REVISION_DIRECTORY_PREFIX)) return null
        val suffix = name.removePrefix(REVISION_DIRECTORY_PREFIX)
        if (suffix.isEmpty() || suffix.any { !it.isDigit() }) return null
        return suffix.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun isValidId(value: String): Boolean {
        return value.matches(Regex("^[0-9a-fA-F-]{36}$"))
    }

    private fun verifyCorpusRevisionContents(
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
                    recordId != CorpusTextNormalizer.sha256(text) ||
                    CorpusTextNormalizer.normalize(text) != text
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
        val recordsFileDatasetSha256 = canonicalDatasetDigestFromRecordFile(recordsFile)
        if (actualDatasetSha256 != recordsFileDatasetSha256 ||
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

    private fun canonicalDatasetDigestFromRecordFile(recordsFile: File): String {
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
            if (reader.readLine() != null ||
                decision.optInt("schemaVersion", -1) != ADMISSION_LOG_SCHEMA_VERSION ||
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
                checks.optString("semanticLeakage") != "PENDING" ||
                decision.optString("validatorVersion") != ADMISSION_VALIDATOR_VERSION ||
                decision.optString("policyVersion") != ADMISSION_POLICY_VERSION ||
                decision.optString("decision") != INITIAL_ADMISSION_DECISION ||
                decision.optString("datasetManifestSha256") != manifestSha256 ||
                decision.optString("datasetSha256") != datasetSha256 ||
                !CanonicalDatasetDigests.isSha256Hex(manifestSha256) ||
                !CanonicalDatasetDigests.isSha256Hex(datasetSha256) ||
                !isValidId(decision.optString("decisionId")) ||
                decision.optString("actor") != "LOCAL_IMPORTER" ||
                decision.optLong("createdAtEpochMillis", 0L) <= 0L
            ) {
                throw IllegalStateException("The staged dataset admission decision is inconsistent.")
            }
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

    companion object {
        private const val INITIAL_REVISION = 1
        private const val REVISION_DIRECTORY_PREFIX = "revision-"
        private const val RECORDS_FILE = CorpusStorageLayout.RECORDS_FILE
        private const val MANIFEST_FILE = CorpusStorageLayout.MANIFEST_FILE
        private const val MANIFEST_DIGEST_FILE = CorpusStorageLayout.MANIFEST_DIGEST_FILE
        private const val ADMISSION_DECISIONS_FILE = CorpusStorageLayout.ADMISSION_DECISIONS_FILE
        private const val STAGING_DIRECTORY = CorpusStorageLayout.STAGING_DIRECTORY
        private const val CORPORA_DIRECTORY = CorpusStorageLayout.CORPORA_DIRECTORY
        private const val STREAM_BUFFER_SIZE = CorpusStorageLayout.STREAM_BUFFER_SIZE
        private const val NEW_LINE_BYTE_VALUE = CorpusStorageLayout.NEW_LINE_BYTE_VALUE
        private const val CARRIAGE_RETURN_BYTE_VALUE = CorpusStorageLayout.CARRIAGE_RETURN_BYTE_VALUE

        private const val IMPORTER_VERSION = CorpusManifestContract.IMPORTER_VERSION
        private const val MANIFEST_SCHEMA_VERSION = CorpusManifestContract.MANIFEST_SCHEMA_VERSION
        private const val PARSER_VERSION = CorpusManifestContract.PARSER_VERSION
        private const val NORMALIZATION_VERSION = CorpusManifestContract.NORMALIZATION_VERSION
        private const val DEDUPLICATION_VERSION = CorpusManifestContract.DEDUPLICATION_VERSION
        private const val SPLIT_ALGORITHM = CorpusManifestContract.SPLIT_ALGORITHM
        private const val SPLIT_SEED = CorpusManifestContract.SPLIT_SEED
        private const val VALIDATION_RATIO = CorpusManifestContract.VALIDATION_RATIO
        private const val TEST_RATIO = CorpusManifestContract.TEST_RATIO
        private const val EXCLUDED_DUPLICATE_ONLY_SPLIT =
            CorpusManifestContract.EXCLUDED_DUPLICATE_ONLY_SPLIT

        private const val ADMISSION_LOG_SCHEMA_VERSION =
            CorpusManifestContract.ADMISSION_LOG_SCHEMA_VERSION
        private const val ADMISSION_VALIDATOR_VERSION =
            CorpusManifestContract.ADMISSION_VALIDATOR_VERSION
        private const val ADMISSION_POLICY_VERSION =
            CorpusManifestContract.ADMISSION_POLICY_VERSION
        private const val INITIAL_ADMISSION_DECISION =
            CorpusManifestContract.INITIAL_ADMISSION_DECISION
        private const val LEGACY_UNVERIFIED_ADMISSION =
            CorpusManifestContract.LEGACY_UNVERIFIED_ADMISSION
        private const val UNKNOWN_ADMISSION = CorpusManifestContract.UNKNOWN_ADMISSION
        private val ADMISSION_DECISIONS = CorpusManifestContract.ADMISSION_DECISIONS
    }
}
