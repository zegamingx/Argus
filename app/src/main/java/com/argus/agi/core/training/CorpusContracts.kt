package com.argus.agi.core.training

/**
 * Immutable result of staging a user-selected local corpus.
 *
 * A preview is not a committed dataset, admission approval or training request.
 */
data class CorpusImportPreview(
    val stageId: String,
    val corpusId: String,
    val selectedSourceCount: Int,
    val parsedDocumentCount: Int,
    val acceptedRecordCount: Long,
    val duplicateRecordCount: Long,
    val rejectedRecordCount: Long,
    val skippedFileCount: Int,
    val importedBytes: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val warnings: List<String>
)

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

/**
 * Platform-neutral ingress port for user-selected and bundled local corpus inputs.
 *
 * Android URI values cross this boundary as opaque strings; this contract does
 * not expose android.net.Uri or ContentResolver.
 */
interface CorpusImportStager {
    suspend fun stageUserFiles(
        sourceUriReferences: List<String>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview

    suspend fun stageBundledArgusId(purposes: Set<DatasetPurpose>): CorpusImportPreview

    suspend fun stageBundledDigiTransId(purposes: Set<DatasetPurpose>): CorpusImportPreview
}

/**
 * Persistence port for staged and committed corpus revisions.
 *
 * Implementations own manifest bytes, admission-log persistence, immutable
 * revision publication and integrity verification. No training authorization
 * is implied by any operation in this port.
 */
interface LocalCorpusStore {
    fun writeStagedManifest(stageId: String, manifestJson: String): String

    fun writeInitialAdmissionDecision(
        stageId: String,
        manifestSha256: String,
        datasetSha256: String
    )

    fun commit(stageId: String): LocalCorpusSummary

    fun verifyCommittedCorpusIntegrity(
        corpusId: String,
        revision: Int
    ): LocalCorpusIntegrityReport

    fun discard(stageId: String)

    fun listCommitted(): List<LocalCorpusSummary>
}

