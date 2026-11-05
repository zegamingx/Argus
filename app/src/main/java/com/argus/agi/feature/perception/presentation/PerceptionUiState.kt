package com.argus.agi.feature.perception.presentation

/**
 * Immutable presentation state for the Perception feature.
 *
 * Hardware values are descriptive device observations, not proof that a
 * particular accelerator is selected or executing model operations.
 */
data class PerceptionUiState(
    val hardwareInfo: PerceptionHardwareUiState = PerceptionHardwareUiState(),
    val currentContextSummary: String = "",
    val cameraLuminance: Float = 0f,
    val isAccessibilityConnected: Boolean = false
)

data class PerceptionHardwareUiState(
    val socManufacturer: String = "Unknown",
    val socModel: String = "Unknown",
    val primaryAccelerator: String = "Unknown",
    val npuApuDetected: String = "Unknown",
    val vulkanSupported: Boolean = false,
    val vulkanVersionCode: Int = 0,
    val cpuCores: Int = 0,
    val maxHeapMb: Long = 0L
)
