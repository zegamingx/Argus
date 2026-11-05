package com.argus.agi.application.runtime

/**
 * Platform capture handoff for transient request/session context.
 *
 * This port does not authorize collection, persistence or training admission.
 */
interface CaptureRuntime {
    fun isPackageExcluded(packageName: String): Boolean

    /**
     * Updates process-local context for the current request/session only.
     * This operation must not persist personal content or create a training candidate.
     */
    fun updateTransientScreenContext(packageName: String, visibleText: String)
}
