package com.argus.agi.data.corpus

import com.argus.agi.core.training.CanonicalDatasetDigests
import java.nio.charset.StandardCharsets
import java.text.Normalizer

/**
 * Versioned corpus-text identity helpers shared by ingestion and persisted-revision
 * verification. The version identifier remains part of CorpusManifestContract.
 */
internal object CorpusTextNormalizer {

    fun normalize(source: String): String {
        val nfc = Normalizer.normalize(source, Normalizer.Form.NFC)
        return nfc
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .joinToString("\n") { it.trimEnd() }
            .trim()
    }

    fun sha256(source: String): String {
        return CanonicalDatasetDigests.sha256Hex(
            source.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
