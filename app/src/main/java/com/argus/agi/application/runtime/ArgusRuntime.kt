package com.argus.agi.application.runtime

import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow application-facing runtime ports used by Android entry adapters.
 *
 * This contract intentionally exposes operations and snapshots, not concrete
 * Android services, storage implementations, model engines, or Compose state.
 */
interface ArgusRuntimeOwner {
    val argusRuntime: ArgusRuntime
}

interface ArgusRuntime {
    val training: TrainingRuntime
    val device: DeviceRuntime
    val hardware: HardwareRuntime
    val capture: CaptureRuntime
    val accessibility: AccessibilityRuntime
}

data class TrainingStatusSnapshot(
    val isPaused: Boolean,
    val currentSource: String?,
    val latestLoss: Float?
)

interface TrainingRuntime {
    fun snapshot(): TrainingStatusSnapshot
    fun togglePause()
}

data class DeviceSafetySnapshot(
    val batteryPercent: Int,
    val temperatureCelsius: Float,
    val isSafeToTrain: Boolean,
    val statusMessage: String
)

interface DeviceRuntime {
    fun snapshot(): DeviceSafetySnapshot
}

interface HardwareRuntime {
    fun primaryAcceleratorLabel(): String
}

interface CaptureRuntime {
    fun isPackageExcluded(packageName: String): Boolean

    /**
     * Updates process-local context for the current request/session only.
     * This operation must not persist personal content or create a training candidate.
     */
    fun updateTransientScreenContext(packageName: String, visibleText: String)
}

interface AccessibilityActionController {
    fun performClickAction(query: String): Boolean
}

interface AccessibilityRuntime {
    val isConnected: StateFlow<Boolean>
    fun attach(controller: AccessibilityActionController)
    fun detach(controller: AccessibilityActionController)
    fun performClickAction(query: String): Boolean
}
