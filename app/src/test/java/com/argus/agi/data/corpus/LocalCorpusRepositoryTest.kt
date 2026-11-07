package com.argus.agi.data.corpus

import com.argus.agi.core.training.CanonicalDatasetDigests
import com.argus.agi.core.training.DatasetSplitMembership
import java.io.File
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalCorpusRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun commitAndVerifyPreserveCanonicalDatasetAndAdmissionIdentity() {
        val filesDirectory = temporaryFolder.newFolder("app-files")
        val repository = LocalCorpusRepository(filesDirectory)
        val fixture = createValidStagedCorpus(filesDirectory)

        val committed = repository.commit(fixture.stageId)

        assertEquals(fixture.corpusId, committed.corpusId)
        assertEquals(1, committed.revision)
        assertEquals(1L, committed.recordCount)
        assertEquals("PENDING", committed.lastAdmissionDecision)

        val report = repository.verifyCommittedCorpusIntegrity(
            corpusId = fixture.corpusId,
            revision = 1
        )

        assertEquals(fixture.datasetSha256, report.canonicalDatasetSha256)
        assertEquals(fixture.splitManifestSha256, report.splitManifestSha256)
        assertEquals(1L, report.recordCount)
        assertEquals(1L, report.trainRecordCount)
        assertEquals(0L, report.validationRecordCount)
        assertEquals(0L, report.testRecordCount)
        assertEquals("PENDING", report.lastAdmissionDecision)
        assertTrue(CanonicalDatasetDigests.isSha256Hex(report.manifestSha256))
    }

    @Test(expected = IllegalStateException::class)
    fun commitRejectsRecordBytesChangedAfterAdmissionWasWritten() {
        val filesDirectory = temporaryFolder.newFolder("mutated-app-files")
        val repository = LocalCorpusRepository(filesDirectory)
        val fixture = createValidStagedCorpus(filesDirectory)
        val recordsFile = File(fixture.stageDirectory, CorpusStorageLayout.RECORDS_FILE)
        val tamperedLine = JSONObject()
            .put("recordId", "tampered")
            .put("sourceDocumentId", fixture.sourceDocumentSha256)
            .put("sourceName", "source.txt")
            .put("text", "changed")
            .put("split", "train")
            .toString() + "\n"

        recordsFile.writeText(tamperedLine, Charsets.UTF_8)
        repository.commit(fixture.stageId)
    }

    @Test(expected = IllegalStateException::class)
    fun manifestWriterDoesNotReplaceAnExistingStagedManifest() {
        val filesDirectory = temporaryFolder.newFolder("manifest-app-files")
        val fixture = createValidStagedCorpus(filesDirectory)
        val manifestFile = File(fixture.stageDirectory, CorpusStorageLayout.MANIFEST_FILE)
        val originalManifest = manifestFile.readText(Charsets.UTF_8)
        val repository = LocalCorpusRepository(filesDirectory)

        repository.writeStagedManifest(
            stageId = fixture.stageId,
            manifestJson = originalManifest
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun discardRejectsPathTraversalIdentifiers() {
        val filesDirectory = temporaryFolder.newFolder("path-app-files")
        LocalCorpusRepository(filesDirectory).discard("../outside")
    }

    @Test
    fun listCommittedReportsPendingButDoesNotClaimApproval() {
        val filesDirectory = temporaryFolder.newFolder("listing-app-files")
        val repository = LocalCorpusRepository(filesDirectory)
        val fixture = createValidStagedCorpus(filesDirectory)
        repository.commit(fixture.stageId)

        val listed = repository.listCommitted()

        assertEquals(1, listed.size)
        assertEquals(fixture.corpusId, listed.single().corpusId)
        assertEquals("PENDING", listed.single().lastAdmissionDecision)
        assertNotEquals("APPROVED", listed.single().lastAdmissionDecision)
    }

    private fun createValidStagedCorpus(filesDirectory: File): StagedCorpusFixture {
        val stageId = "d81dcf35-a6fc-4b4f-9e70-0c2f919cc251"
        val corpusId = "cf22c615-6c74-40d1-bcb3-75b33dc5dafe"
        val stageDirectory = File(
            File(filesDirectory, CorpusStorageLayout.STAGING_DIRECTORY),
            stageId
        )
        assertTrue(stageDirectory.mkdirs())

        val sourceName = "source.txt"
        val recordText = "Argus local corpus."
        val sourceBytes = recordText.toByteArray(StandardCharsets.UTF_8)
        val sourceDocumentSha256 = CanonicalDatasetDigests.sha256Hex(sourceBytes)
        val recordId = CanonicalDatasetDigests.sha256Hex(sourceBytes)
        val canonicalLine = CanonicalDatasetDigests.canonicalRecordLine(
            recordId = recordId,
            sourceDocumentId = sourceDocumentSha256,
            sourceName = sourceName,
            text = recordText,
            split = "train"
        )

        val recordsFile = File(stageDirectory, CorpusStorageLayout.RECORDS_FILE)
        recordsFile.writeBytes((canonicalLine + "\n").toByteArray(StandardCharsets.UTF_8))

        val datasetDigest = CanonicalDatasetDigests.newDatasetDigest()
        CanonicalDatasetDigests.updateDatasetRecord(datasetDigest, canonicalLine)
        val datasetSha256 = CanonicalDatasetDigests.finish(datasetDigest)

        val trainDigest = CanonicalDatasetDigests.newMembershipDigest("train")
        CanonicalDatasetDigests.updateMembershipDigest(
            digest = trainDigest,
            recordId = recordId,
            sourceDocumentId = sourceDocumentSha256
        )
        val memberships = mapOf(
            "train" to DatasetSplitMembership(
                recordCount = 1L,
                sha256 = CanonicalDatasetDigests.finish(trainDigest)
            ),
            "validation" to DatasetSplitMembership(
                recordCount = 0L,
                sha256 = emptyMembershipSha256("validation")
            ),
            "test" to DatasetSplitMembership(
                recordCount = 0L,
                sha256 = emptyMembershipSha256("test")
            )
        )

        val splitPolicy = JSONObject()
            .put("algorithm", CorpusManifestContract.SPLIT_ALGORITHM)
            .put("seed", CorpusManifestContract.SPLIT_SEED)
            .put("grouping", CorpusManifestContract.SPLIT_GROUPING)
            .put("validationRatio", CorpusManifestContract.VALIDATION_RATIO)
            .put("testRatio", CorpusManifestContract.TEST_RATIO)

        val membershipsJson = JSONObject()
        for ((splitName, membership) in memberships) {
            membershipsJson.put(
                splitName,
                JSONObject()
                    .put("recordCount", membership.recordCount)
                    .put("sha256", membership.sha256)
            )
        }

        val splitManifestSha256 = CanonicalDatasetDigests.splitManifestSha256(
            parentDatasetSha256 = datasetSha256,
            algorithm = CorpusManifestContract.SPLIT_ALGORITHM,
            seed = CorpusManifestContract.SPLIT_SEED,
            grouping = CorpusManifestContract.SPLIT_GROUPING,
            validationRatio = CorpusManifestContract.VALIDATION_RATIO,
            testRatio = CorpusManifestContract.TEST_RATIO,
            memberships = memberships
        )

        val sourceMetadata = JSONArray().put(
            JSONObject()
                .put("name", sourceName)
                .put("mimeType", "text/plain")
                .put("sizeBytes", sourceBytes.size)
                .put("sha256", sourceDocumentSha256)
                .put("archive", false)
        )
        val documentMetadata = JSONArray().put(
            JSONObject()
                .put("name", sourceName)
                .put("sha256", sourceDocumentSha256)
                .put("sourceArtifactSha256", sourceDocumentSha256)
                .put("sizeBytes", sourceBytes.size)
                .put("mediaType", "text/plain")
                .put("recordCount", 1)
                .put("split", "train")
        )

        val manifest = JSONObject()
            .put("schemaVersion", CorpusManifestContract.MANIFEST_SCHEMA_VERSION)
            .put("corpusId", corpusId)
            .put("revision", 1)
            .put("importedAtEpochMillis", 1_800_000_000_000L)
            .put("importerVersion", CorpusManifestContract.IMPORTER_VERSION)
            .put("recordFormat", CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID)
            .put("purposes", JSONArray().put("model_training"))
            .put("sources", sourceMetadata)
            .put("documents", documentMetadata)
            .put(
                "processing",
                JSONObject()
                    .put("parserVersion", CorpusManifestContract.PARSER_VERSION)
                    .put("normalizationVersion", CorpusManifestContract.NORMALIZATION_VERSION)
                    .put("deduplicationVersion", CorpusManifestContract.DEDUPLICATION_VERSION)
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
                    .put(
                        "canonicalFormat",
                        CanonicalDatasetDigests.CANONICAL_RECORD_FORMAT_ID
                    )
                    .put("canonicalSha256", datasetSha256)
                    .put("recordCount", 1)
            )
            .put(
                "splitManifest",
                JSONObject()
                    .put(
                        "schemaVersion",
                        CanonicalDatasetDigests.SPLIT_MANIFEST_SCHEMA_VERSION
                    )
                    .put("parentDatasetSha256", datasetSha256)
                    .put("policy", splitPolicy)
                    .put("memberships", membershipsJson)
                    .put("sha256", splitManifestSha256)
            )
            .put(
                "admissionDecisionLog",
                JSONObject()
                    .put("format", "jsonl")
                    .put("path", CorpusStorageLayout.ADMISSION_DECISIONS_FILE)
                    .put("appendOnly", true)
            )
            .put(
                "counts",
                JSONObject()
                    .put("acceptedRecords", 1)
                    .put("duplicateRecords", 0)
                    .put("rejectedRecords", 0)
                    .put("skippedFiles", 0)
                    .put("importedBytes", sourceBytes.size)
                    .put("trainRecords", 1)
                    .put("validationRecords", 0)
                    .put("testRecords", 0)
            )
            .put("split", splitPolicy)
            .put("warnings", JSONArray())

        val repository = LocalCorpusRepository(filesDirectory)
        val manifestSha256 = repository.writeStagedManifest(
            stageId = stageId,
            manifestJson = manifest.toString(2)
        )
        repository.writeInitialAdmissionDecision(
            stageId = stageId,
            manifestSha256 = manifestSha256,
            datasetSha256 = datasetSha256
        )

        return StagedCorpusFixture(
            stageId = stageId,
            corpusId = corpusId,
            stageDirectory = stageDirectory,
            sourceDocumentSha256 = sourceDocumentSha256,
            datasetSha256 = datasetSha256,
            splitManifestSha256 = splitManifestSha256
        )
    }

    private fun emptyMembershipSha256(split: String): String {
        return CanonicalDatasetDigests.finish(
            CanonicalDatasetDigests.newMembershipDigest(split)
        )
    }

    private data class StagedCorpusFixture(
        val stageId: String,
        val corpusId: String,
        val stageDirectory: File,
        val sourceDocumentSha256: String,
        val datasetSha256: String,
        val splitManifestSha256: String
    )
}
