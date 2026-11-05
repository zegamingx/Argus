package com.argus.agi.feature.learning.presentation

/**
 * Immutable presentation state for the Learning feature.
 *
 * Queue records are display projections; they are not training-session
 * manifests, candidate manifests or authoritative workflow state.
 */
data class LearningUiState(
    val isPaused: Boolean = false,
    val safetyState: LearningSafetyUiState = LearningSafetyUiState(),
    val currentTrainingItem: TrainingQueueItemUiState? = null,
    val nextQueue: List<TrainingQueueItemUiState> = emptyList(),
    val completedQueue: List<TrainingQueueItemUiState> = emptyList(),
    val lossHistory: List<Float> = emptyList(),
    val benchmark: LearningBenchmarkUiState? = null,
    val isBenchmarking: Boolean = false,
    val hardwareInfo: LearningHardwareUiState = LearningHardwareUiState(),
    val totalStepsExecuted: Int = 0
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
