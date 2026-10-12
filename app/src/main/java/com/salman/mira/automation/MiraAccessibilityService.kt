package com.salman.mira.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Automation engine skeleton. Verified against Maya's config:
 * listens to window-state changes, reads the UI tree, performs gestures.
 *
 * Toolset to implement (mirroring Maya's verified tools):
 *  - openAppByName(name)
 *  - tapText(text)          // label lookup — preferred over coordinates
 *  - typeText(text)
 *  - swipe(x1,y1,x2,y2)
 *  - waitForScreenText(text, timeoutMs)
 *  - pressBack(), scrollForward()
 */
class MiraAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // TODO: feed window snapshots to the task planner
    }

    override fun onInterrupt() {}

    /** Tap the first node whose text or content-description matches. */
    fun tapText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findAccessibilityNodeInfosByText(text).firstOrNull()
            ?: findByDescription(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** Type into the currently focused editable field. */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
            )
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Raw tap by coordinates (fallback when no label exists). */
    fun tapAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
                .build(), null, null
        )
    }

    private fun findByDescription(
        node: AccessibilityNodeInfo, text: String
    ): AccessibilityNodeInfo? {
        if (node.contentDescription?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findByDescription(it, text)?.let { found -> return found } }
        }
        return null
    }
}
