package com.argus.agi.core.training

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class DatasetSplitMembership(
    val recordCount: Long,
    val sha256: String
)

/**
 * Versioned, platform-neutral encoding for the normalized JSONL corpus consumed by
 * the local dataset reader. Field order and JSON escaping are explicit so the
 * resulting identity does not depend on JSONObject implementation details.
 */
object CanonicalDatasetDigests {

    const val CANONICAL_RECORD_FORMAT_ID = "argus-jsonl-canonical-v1"
    const val SPLIT_MANIFEST_SCHEMA_VERSION = 1

    val SPLIT_NAMES: List<String> = listOf("train", "validation", "test")

    private val datasetDomain = "ARGUS_CANONICAL_DATASET_V1\n".toByteArray(StandardCharsets.UTF_8)
    private val membershipDomain =
        "ARGUS_SPLIT_MEMBERSHIP_V1\u0000".toByteArray(StandardCharsets.UTF_8)
    private val splitManifestDomain =
        "ARGUS_SPLIT_MANIFEST_V1\u0000".toByteArray(StandardCharsets.UTF_8)
    private val hexDigits = "0123456789abcdef".toCharArray()

    fun newDatasetDigest(): MessageDigest {
        return MessageDigest.getInstance("SHA-256").apply {
            update(datasetDomain)
        }
    }

    fun canonicalRecordLine(
        recordId: String,
        sourceDocumentId: String,
        sourceName: String,
        text: String,
        split: String
    ): String {
        require(recordId.isNotBlank()) { "A canonical dataset record must have a record ID." }
        require(sourceDocumentId.isNotBlank()) {
            "A canonical dataset record must have a source-document ID."
        }
        require(sourceName.isNotBlank()) { "A canonical dataset record must have a source name." }
        require(split in SPLIT_NAMES) { "The dataset split is not recognized." }

        return buildString {
            append("{\"recordId\":")
            appendJsonString(this, recordId)
            append(",\"sourceDocumentId\":")
            appendJsonString(this, sourceDocumentId)
            append(",\"sourceName\":")
            appendJsonString(this, sourceName)
            append(",\"text\":")
            appendJsonString(this, text)
            append(",\"split\":")
            appendJsonString(this, split)
            append('}')
        }
    }

    fun updateDatasetRecord(digest: MessageDigest, canonicalRecordLine: String) {
        digest.update(canonicalRecordLine.toByteArray(StandardCharsets.UTF_8))
        digest.update(NEW_LINE_BYTE)
    }

    fun newMembershipDigest(split: String): MessageDigest {
        require(split in SPLIT_NAMES) { "The dataset split is not recognized." }
        return MessageDigest.getInstance("SHA-256").apply {
            update(membershipDomain)
            updateLengthPrefixed(this, split)
        }
    }

    fun updateMembershipDigest(
        digest: MessageDigest,
        recordId: String,
        sourceDocumentId: String
    ) {
        require(recordId.isNotBlank()) { "A split member must have a record ID." }
        require(sourceDocumentId.isNotBlank()) { "A split member must have a source-document ID." }
        updateLengthPrefixed(digest, recordId)
        updateLengthPrefixed(digest, sourceDocumentId)
    }

    fun splitManifestSha256(
        parentDatasetSha256: String,
        algorithm: String,
        seed: String,
        grouping: String,
        validationRatio: Double,
        testRatio: Double,
        memberships: Map<String, DatasetSplitMembership>
    ): String {
        require(parentDatasetSha256.isNotBlank())
        require(algorithm.isNotBlank())
        require(seed.isNotBlank())
        require(grouping.isNotBlank())
        require(validationRatio in 0.0..1.0)
        require(testRatio in 0.0..1.0)
        require(validationRatio + testRatio < 1.0)

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(splitManifestDomain)
        updateLengthPrefixed(digest, parentDatasetSha256)
        updateLengthPrefixed(digest, algorithm)
        updateLengthPrefixed(digest, seed)
        updateLengthPrefixed(digest, grouping)
        updateLong(digest, java.lang.Double.doubleToLongBits(validationRatio))
        updateLong(digest, java.lang.Double.doubleToLongBits(testRatio))

        for (split in SPLIT_NAMES) {
            val membership = memberships[split]
                ?: throw IllegalArgumentException("A split membership is missing: " + split)
            require(membership.recordCount >= 0L)
            require(isSha256Hex(membership.sha256))

            updateLengthPrefixed(digest, split)
            updateLong(digest, membership.recordCount)
            updateLengthPrefixed(digest, membership.sha256)
        }

        return finish(digest)
    }

    fun sha256Hex(bytes: ByteArray): String {
        return finish(MessageDigest.getInstance("SHA-256").apply { update(bytes) })
    }

    fun finish(digest: MessageDigest): String {
        val bytes = digest.digest()
        val output = CharArray(bytes.size * 2)
        for (index in bytes.indices) {
            val value = bytes[index].toInt() and 0xff
            output[index * 2] = hexDigits[value ushr 4]
            output[index * 2 + 1] = hexDigits[value and 0x0f]
        }
        return String(output)
    }

    fun isSha256Hex(value: String): Boolean {
        return value.length == SHA256_HEX_LENGTH && value.all { character ->
            character in '0'..'9' || character in 'a'..'f'
        }
    }

    private fun updateLengthPrefixed(digest: MessageDigest, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 0)
        updateInt(digest, bytes.size)
        digest.update(bytes)
    }

    private fun updateInt(digest: MessageDigest, value: Int) {
        digest.update((value ushr 24).toByte())
        digest.update((value ushr 16).toByte())
        digest.update((value ushr 8).toByte())
        digest.update(value.toByte())
    }

    private fun updateLong(digest: MessageDigest, value: Long) {
        for (shift in 56 downTo 0 step 8) {
            digest.update((value ushr shift).toByte())
        }
    }

    private fun appendJsonString(output: StringBuilder, value: String) {
        output.append('"')
        var index = 0
        while (index < value.length) {
            val character = value[index]
            when (character) {
                '"' -> output.append("\\\"")
                '\\' -> output.append("\\\\")
                '\b' -> output.append("\\b")
                '\u000c' -> output.append("\\f")
                '\n' -> output.append("\\n")
                '\r' -> output.append("\\r")
                '\t' -> output.append("\\t")
                else -> {
                    when {
                        character.code < 0x20 -> {
                            output.append("\\u00")
                            output.append(hexDigits[(character.code ushr 4) and 0x0f])
                            output.append(hexDigits[character.code and 0x0f])
                        }
                        character.isHighSurrogate() -> {
                            require(index + 1 < value.length && value[index + 1].isLowSurrogate()) {
                                "Canonical JSON text contains an unpaired high surrogate."
                            }
                            output.append(character)
                            output.append(value[index + 1])
                            index += 1
                        }
                        character.isLowSurrogate() -> {
                            throw IllegalArgumentException(
                                "Canonical JSON text contains an unpaired low surrogate."
                            )
                        }
                        else -> output.append(character)
                    }
                }
            }
            index += 1
        }
        output.append('"')
    }

    private const val SHA256_HEX_LENGTH = 64
    private val NEW_LINE_BYTE = '\n'.code.toByte()
}
