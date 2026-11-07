package com.argus.agi.core.training

/**
 * Relative file names that form part of the on-device corpus storage format.
 */
internal object CorpusStorageLayout {
    const val STAGING_DIRECTORY = "corpus-staging"
    const val CORPORA_DIRECTORY = "corpora"
    const val MANIFEST_FILE = "manifest.json"
    const val MANIFEST_DIGEST_FILE = "manifest.sha256"
    const val ADMISSION_DECISIONS_FILE = "admission-decisions.jsonl"
    const val RECORDS_FILE = "records.jsonl"
    const val STREAM_BUFFER_SIZE = 64 * 1024
    const val NEW_LINE_BYTE_VALUE = 0x0a
    const val CARRIAGE_RETURN_BYTE_VALUE = 0x0d
}
