package com.argus.agi.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.argus.agi.app.entry.ArgusApp
import com.argus.agi.sensor.ExcludedAppsManager

class ArgusAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ArgusAccessibilityService? = null
            private set

        fun performClickAction(query: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
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
    }

    private lateinit var excludedAppsManager: ExcludedAppsManager
    private var lastScreenTextHash = 0
    private var lastPerceptionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        excludedAppsManager = ExcludedAppsManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkgName = event.packageName?.toString() ?: return

        // PRIVACY ENFORCEMENT: Never read excluded sensitive apps
        if (excludedAppsManager.isAppExcluded(pkgName)) {
            return
        }

        // Throttle screen reading to prevent overwhelming the training pipeline
        val now = System.currentTimeMillis()
        if (now - lastPerceptionTime < 2500) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val gatheredText = StringBuilder()
        traverseNodes(rootNode, gatheredText)

        val fullText = gatheredText.toString().trim()
        if (fullText.length > 20) {
            val currentHash = fullText.hashCode()
            if (currentHash != lastScreenTextHash) {
                lastScreenTextHash = currentHash
                lastPerceptionTime = now

                // Update world model & enqueue continuous training
                ArgusApp.worldModel.updateScreenContext(pkgName, fullText)
                ArgusApp.trainingManager.enqueueData(
                    source = "Screen ($pkgName)",
                    title = "Screen: ${pkgName.substringAfterLast('.')}",
                    content = fullText.take(400)
                )
            }
        }
    }

    override fun onInterrupt() {}

    private fun traverseNodes(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            sb.append(text).append(" ")
        } else if (!desc.isNullOrBlank()) {
            sb.append(desc).append(" ")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNodes(child, sb)
        }
    }
}
