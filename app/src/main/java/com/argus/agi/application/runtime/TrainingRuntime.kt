package com.argus.agi.application.runtime

/**
 * Read-only projection of the current prototype training status.
 */
data class TrainingStatusSnapshot(
    val isPaused: Boolean,
    val currentSource: String?,
    val latestLoss: Float?
)

/**
 * Narrow control and observation port used by the Android foreground adapter.
 */
interface TrainingRuntime {
    fun snapshot(): TrainingStatusSnapshot
    fun togglePause()
}
