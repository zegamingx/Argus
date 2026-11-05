package com.argus.agi.application.runtime

/**
 * Read-only availability projection for the Android foreground notification.
 */
data class TrainingStatusSnapshot(
    val isWorkflowAvailable: Boolean,
    val statusMessage: String
)

/**
 * Read-only observation port used by the Android foreground adapter.
 */
interface TrainingRuntime {
    fun snapshot(): TrainingStatusSnapshot
}
