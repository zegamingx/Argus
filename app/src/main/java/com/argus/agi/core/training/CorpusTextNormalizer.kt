package com.argus.agi.core.training

import java.nio.charset.StandardCharsets
import java.text.Normalizer

/**
 * Canonical corpus text transformation and record identity used by both the
 * Android import adapter and persisted-corpus verification.
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
