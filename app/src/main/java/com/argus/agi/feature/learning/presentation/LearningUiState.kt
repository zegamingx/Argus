package com.argus.agi.feature.learning.presentation

/**
 * Immutable presentation state for the Learning feature.
 *
 * Queue records are display projections; they are not training-session
 * manifests, candidate manifests or authoritative workflow state.
 */
data class LearningUiState(
    val isWorkflowAvailable: Boolean = false,
    val workflowStatusMessage: String = "Learning is disabled until an approved candidate-manifest workflow is implemented.",
    val safetyState: LearningSafetyUiState = LearningSafetyUiState(),
    val currentTrainingItem: TrainingQueueItemUiState? = null,
    val nextQueue: List<TrainingQueueItemUiState> = emptyList(),
    val completedQueue: List<TrainingQueueItemUiState> = emptyList(),
    val lossHistory: List<Float> = emptyList(),
    val benchmark: LearningBenchmarkUiState? = null,
    val isBenchmarking: Boolean = false,
    val hardwareInfo: LearningHardwareUiState = LearningHardwareUiState(),
    val totalStepsExecuted: Int = 0,
    val datasetImport: DatasetImportUiState = DatasetImportUiState()
)

data class LearningSafetyUiState(
    val isSafeToTrain: Boolean = false,
    val statusMessage: String = "Device safety state is initializing."
)

data class LearningHardwareUiState(
    val primaryAccelerator: String = "Unknown"
)

data class TrainingQueueItemUiState(
    val id: String,
    val source: String,
    val title: String,
    val content: String,
    val finalLoss: Float?,
    val stepDurationMs: Long
)

data class LearningBenchmarkUiState(
    val tokensPerSecond: Float,
    val latencyMsPerToken: Float,
    val memoryUsageMb: Float
)

enum class DatasetPurposeUi(val label: String) {
    MODEL_TRAINING("General model training"),
    TOKENIZER_FREQUENCY_FITTING("Tokenizer frequency fitting"),
    ALIGNED_TOKENIZER_DEVELOPMENT("Aligned tokenizer development")
}

data class DatasetImportUiState(
    val isBusy: Boolean = false,
    val statusMessage: String = "Imported files are staged locally before a corpus revision is committed.",
    val errorMessage: String? = null,
    val selectedPurposes: Set<DatasetPurposeUi> = setOf(
        DatasetPurposeUi.MODEL_TRAINING,
        DatasetPurposeUi.TOKENIZER_FREQUENCY_FITTING
    ),
    val stagedPreview: DatasetImportPreviewUiState? = null,
    val committedCorpora: List<LocalCorpusUiState> = emptyList()
)

data class DatasetImportPreviewUiState(
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

data class LocalCorpusUiState(
    val corpusId: String,
    val revision: Int,
    val importedAtEpochMillis: Long,
    val recordCount: Long,
    val trainRecordCount: Long,
    val validationRecordCount: Long,
    val testRecordCount: Long,
    val purposeLabels: List<String>
)
