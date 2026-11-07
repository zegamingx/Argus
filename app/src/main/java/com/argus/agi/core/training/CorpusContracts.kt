package com.argus.agi.core.training

/**
 * Declares which local workflow may consume a user-imported corpus.
 *
 * These values are persisted in the immutable corpus manifest and must remain
 * stable across app versions.
 */
enum class DatasetPurpose(val manifestValue: String) {
    MODEL_TRAINING("model_training"),
    TOKENIZER_FREQUENCY_FITTING("tokenizer_frequency_fitting"),
    ALIGNED_TOKENIZER_DEVELOPMENT("aligned_tokenizer_development")
}

/**
 * Read-only summary of one committed local corpus revision.
 *
 * Admission state is reported separately from training readiness.
 */
data class LocalCorpusSummary(
    val corpusId: String,
    val revision: Int,
    val importedAtEpochMillis: Long,
    val recordCount: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val purposes: Set<DatasetPurpose>,
    val lastAdmissionDecision: String = "UNKNOWN"
)

/**
 * Result of recomputing the immutable identity of a committed corpus revision.
 *
 * A successful integrity report does not imply admission approval or permission
 * to start a training session.
 */
data class LocalCorpusIntegrityReport(
    val corpusId: String,
    val revision: Int,
    val manifestSha256: String,
    val canonicalDatasetSha256: String,
    val splitManifestSha256: String,
    val recordCount: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val lastAdmissionDecision: String,
    val verifiedAtEpochMillis: Long
)
