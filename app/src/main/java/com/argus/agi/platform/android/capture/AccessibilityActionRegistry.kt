package com.argus.agi.platform.android.capture

import com.argus.agi.application.runtime.AccessibilityActionController
import com.argus.agi.application.runtime.AccessibilityRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-local bridge between the Android AccessibilityService lifecycle
 * and application/presentation consumers. It never owns capture policy.
 */
class AccessibilityActionRegistry : AccessibilityRuntime {

    private val controller = AtomicReference<AccessibilityActionController?>(null)
    private val _isConnected = MutableStateFlow(false)

    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun attach(controller: AccessibilityActionController) {
        this.controller.set(controller)
        _isConnected.value = true
    }

    override fun detach(controller: AccessibilityActionController) {
        if (this.controller.compareAndSet(controller, null)) {
            _isConnected.value = false
        }
    }

    override fun performClickAction(query: String): Boolean {
        if (query.isBlank()) return false
        return controller.get()?.performClickAction(query) ?: false
    }
}
