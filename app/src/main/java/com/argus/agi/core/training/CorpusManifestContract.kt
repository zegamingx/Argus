package com.argus.agi.core.training

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
