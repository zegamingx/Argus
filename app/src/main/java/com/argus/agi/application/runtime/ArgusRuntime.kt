package com.argus.agi.application.runtime

/**
 * Application-facing runtime aggregation consumed by Android entry adapters.
 *
 * This remains a transitional composition contract. Feature workflows must
 * migrate to their owning application APIs before this aggregate is retired.
 */
interface ArgusRuntimeOwner {
    val argusRuntime: ArgusRuntime
}

/**
 * Process-scoped set of runtime ports exposed to Android platform adapters.
 */
interface ArgusRuntime {
    val training: TrainingRuntime
    val device: DeviceRuntime
    val hardware: HardwareRuntime
    val capture: CaptureRuntime
    val accessibility: AccessibilityRuntime
}
