package com.argus.agi.core.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalDatasetDigestsTest {

    @Test
    fun canonicalRecordUsesStableFieldOrderAndJsonEscaping() {
        val line = CanonicalDatasetDigests.canonicalRecordLine(
            recordId = "record-id",
            sourceDocumentId = "document-id",
            sourceName = "quoted\"source.txt",
            text = "Baris pertama\nBaris kedua\t😀",
            split = "train"
        )

        assertEquals(
            "{\"recordId\":\"record-id\",\"sourceDocumentId\":\"document-id\"," +
                "\"sourceName\":\"quoted\\\"source.txt\"," +
                "\"text\":\"Baris pertama\\nBaris kedua\\t😀\",\"split\":\"train\"}",
            line
        )
    }

    @Test
    fun datasetDigestChangesWhenConsumedRecordChanges() {
        val original = datasetDigest(text = "Argus lokal", split = "train")
        val changedText = datasetDigest(text = "Argus pribadi", split = "train")
        val changedSplit = datasetDigest(text = "Argus lokal", split = "validation")

        assertNotEquals(original, changedText)
        assertNotEquals(original, changedSplit)
        assertTrue(CanonicalDatasetDigests.isSha256Hex(original))
    }

    @Test
    fun splitMembershipUsesLengthPrefixedIdentifiers() {
        val first = membershipDigest(
            split = "train",
            recordId = "ab",
            sourceDocumentId = "c"
        )
        val second = membershipDigest(
            split = "train",
            recordId = "a",
            sourceDocumentId = "bc"
        )
        val otherSplit = membershipDigest(
            split = "validation",
            recordId = "ab",
            sourceDocumentId = "c"
        )

        assertNotEquals(first, second)
        assertNotEquals(first, otherSplit)
    }

    @Test
    fun splitManifestDigestBindsDatasetPolicyAndMemberships() {
        val baseMemberships = mapOf(
            "train" to DatasetSplitMembership(
                recordCount = 2L,
                sha256 = membershipDigest("train", "r1", "d1")
            ),
            "validation" to DatasetSplitMembership(
                recordCount = 1L,
                sha256 = membershipDigest("validation", "r2", "d2")
            ),
            "test" to DatasetSplitMembership(
                recordCount = 1L,
                sha256 = membershipDigest("test", "r3", "d3")
            )
        )

        val baseline = splitManifestDigest(
            parentDatasetSha256 = "a".repeat(64),
            memberships = baseMemberships
        )
        val changedDataset = splitManifestDigest(
            parentDatasetSha256 = "b".repeat(64),
            memberships = baseMemberships
        )
        val changedMemberships = baseMemberships.toMutableMap().apply {
            this["train"] = DatasetSplitMembership(
                recordCount = 2L,
                sha256 = membershipDigest("train", "r1", "different-document")
            )
        }
        val changedSplit = splitManifestDigest(
            parentDatasetSha256 = "a".repeat(64),
            memberships = changedMemberships
        )

        assertNotEquals(baseline, changedDataset)
        assertNotEquals(baseline, changedSplit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun canonicalRecordRejectsUnknownSplit() {
        CanonicalDatasetDigests.canonicalRecordLine(
            recordId = "record",
            sourceDocumentId = "document",
            sourceName = "source.txt",
            text = "content",
            split = "unknown"
        )
    }

    private fun datasetDigest(text: String, split: String): String {
        val digest = CanonicalDatasetDigests.newDatasetDigest()
        val line = CanonicalDatasetDigests.canonicalRecordLine(
            recordId = "record-id",
            sourceDocumentId = "document-id",
            sourceName = "source.txt",
            text = text,
            split = split
        )
        CanonicalDatasetDigests.updateDatasetRecord(digest, line)
        return CanonicalDatasetDigests.finish(digest)
    }

    private fun membershipDigest(
        split: String,
        recordId: String,
        sourceDocumentId: String
    ): String {
        val digest = CanonicalDatasetDigests.newMembershipDigest(split)
        CanonicalDatasetDigests.updateMembershipDigest(
            digest = digest,
            recordId = recordId,
            sourceDocumentId = sourceDocumentId
        )
        return CanonicalDatasetDigests.finish(digest)
    }

    private fun splitManifestDigest(
        parentDatasetSha256: String,
        memberships: Map<String, DatasetSplitMembership>
    ): String {
        return CanonicalDatasetDigests.splitManifestSha256(
            parentDatasetSha256 = parentDatasetSha256,
            algorithm = "test-split-v1",
            seed = "fixed-seed",
            grouping = "source-document",
            validationRatio = 0.1,
            testRatio = 0.1,
            memberships = memberships
        )
    }
}
