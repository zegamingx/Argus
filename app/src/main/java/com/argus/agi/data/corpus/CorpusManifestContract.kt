package com.argus.agi.data.corpus

/**
 * Immutable version and policy identifiers shared by corpus creation and verification.
 *
 * Changing any value that changes canonical bytes or admission semantics requires
 * an explicit manifest/version migration.
 */
internal object CorpusManifestContract {
    const val IMPORTER_VERSION = "3.0.0"
    const val MANIFEST_SCHEMA_VERSION = 3

    const val PARSER_VERSION = "argus-document-parser-v1"
    const val NORMALIZATION_VERSION = "argus-unicode-nfc-lf-trim-v1"
    const val DEDUPLICATION_VERSION = "sha256-normalized-record-text-v1"

    const val SPLIT_ALGORITHM = "sha256-source-document-v1"
    const val SPLIT_SEED = "argus-local-corpus-split-v1"
    const val SPLIT_GROUPING = "source-document"
    const val VALIDATION_RATIO = 0.10
    const val TEST_RATIO = 0.10
    const val EXCLUDED_DUPLICATE_ONLY_SPLIT = "EXCLUDED_DUPLICATE_ONLY"

    const val ADMISSION_LOG_SCHEMA_VERSION = 1
    const val ADMISSION_VALIDATOR_VERSION = "local-import-integrity-v1"
    const val ADMISSION_POLICY_VERSION = "dataset-admission-pending-v1"
    const val INITIAL_ADMISSION_DECISION = "PENDING"
    const val LEGACY_UNVERIFIED_ADMISSION = "LEGACY_UNVERIFIED"
    const val UNKNOWN_ADMISSION = "UNKNOWN"
    val ADMISSION_DECISIONS = setOf("PENDING", "APPROVED", "REJECTED")
}

/**
 * Private app-storage layout for immutable local corpus revisions and staging.
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
