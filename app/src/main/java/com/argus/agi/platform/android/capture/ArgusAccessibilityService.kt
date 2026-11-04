package com.argus.agi.platform.android.capture

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.argus.agi.application.runtime.AccessibilityActionController
import com.argus.agi.application.runtime.ArgusRuntime
import com.argus.agi.application.runtime.ArgusRuntimeOwner

class ArgusAccessibilityService : AccessibilityService(), AccessibilityActionController {

    private var argusRuntime: ArgusRuntime? = null
    private var lastScreenTextHash = 0
    private var lastPerceptionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val runtime = (application as? ArgusRuntimeOwner)?.argusRuntime ?: return
        argusRuntime = runtime
        runtime.accessibility.attach(this)
    }

    override fun onDestroy() {
        argusRuntime?.accessibility?.detach(this)
        argusRuntime = null
        super.onDestroy()
    }

    override fun performClickAction(query: String): Boolean {
        if (query.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(query)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val runtime = argusRuntime ?: return
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (runtime.capture.isPackageExcluded(packageName)) return

        val now = System.currentTimeMillis()
        if (now - lastPerceptionTime < 2500L) return

        val rootNode = rootInActiveWindow ?: return
        val gatheredText = StringBuilder()
        traverseNodes(rootNode, gatheredText)

        val fullText = gatheredText.toString().trim()
        if (fullText.length <= 20) return

        val currentHash = fullText.hashCode()
        if (currentHash == lastScreenTextHash) return

        lastScreenTextHash = currentHash
        lastPerceptionTime = now

        // Keep this provisional capture path transient. Personal training requires
        // the separate consent, review, provenance and admission workflow.
        runtime.capture.updateTransientScreenContext(packageName, fullText)
    }

    override fun onInterrupt() = Unit

    private fun traverseNodes(node: AccessibilityNodeInfo?, output: StringBuilder) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        val description = node.contentDescription?.toString()?.trim()

        when {
            !text.isNullOrBlank() -> output.append(text).append(' ')
            !description.isNullOrBlank() -> output.append(description).append(' ')
        }

        for (index in 0 until node.childCount) {
            traverseNodes(node.getChild(index), output)
        }
    }
}
