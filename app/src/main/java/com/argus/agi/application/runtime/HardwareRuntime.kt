package com.argus.agi.application.runtime

/**
 * Minimal hardware label projection used for foreground status presentation.
 */
interface HardwareRuntime {
    fun primaryAcceleratorLabel(): String
}
