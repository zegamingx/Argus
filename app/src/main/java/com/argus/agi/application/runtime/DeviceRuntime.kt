package com.argus.agi.application.runtime

/**
 * Read-only device-safety projection used by the Android foreground adapter.
 */
data class DeviceSafetySnapshot(
    val batteryPercent: Int,
    val temperatureCelsius: Float,
    val isSafeToTrain: Boolean,
    val statusMessage: String
)

/**
 * Narrow observation port; resource admission remains owned by application workflows.
 */
interface DeviceRuntime {
    fun snapshot(): DeviceSafetySnapshot
}
