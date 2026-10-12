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

    /** Launch an app by its display name (best effort via launcher query). */
    fun openApp(appName: String): Boolean {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(
            pm.getInstalledApplications(0)
                .firstOrNull { it.loadLabel(pm).toString().contains(appName, ignoreCase = true) }
                ?.packageName ?: return false
        ) ?: return false
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        return true
    }

    /** Wait until the given text appears on screen (polling). */
    fun waitForText(text: String, timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val root = rootInActiveWindow
            if (root != null &&
                (root.findAccessibilityNodeInfosByText(text).isNotEmpty() ||
                        findByDescription(root, text) != null)
            ) return true
            Thread.sleep(400)
        }
        return false
    }

    fun pressBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        return scrollRecursive(root)
    }

    private fun scrollRecursive(node: AccessibilityNodeInfo): Boolean {
        if (node.isScrollable &&
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        ) return true
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { if (scrollRecursive(it)) return true }
        }
        return false
    }

    companion object {
        @Volatile var instance: MiraAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance == this) instance = null
        return super.onUnbind(intent)
    }
}
