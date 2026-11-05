package com.argus.agi.application.runtime

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral controller contract implemented by the live Android
 * AccessibilityService instance while it is connected.
 */
interface AccessibilityActionController {
    fun performClickAction(query: String): Boolean
}

/**
 * Process-local accessibility connection and action port.
 */
interface AccessibilityRuntime {
    val isConnected: StateFlow<Boolean>
    fun attach(controller: AccessibilityActionController)
    fun detach(controller: AccessibilityActionController)
    fun performClickAction(query: String): Boolean
}
