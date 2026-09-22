package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class SweatyAccessibilityService : AccessibilityService() {

    private val isPanicStopped = AtomicBoolean(false)

    companion object {
        @Volatile
        var instance: SweatyAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastActionStatus = MutableStateFlow<String?>(null)
        val lastActionStatus: StateFlow<String?> = _lastActionStatus.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        _lastActionStatus.value = "Sweaty Accessibility Service Connected"
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events processed if screen change tracking needed
    }

    override fun onInterrupt() {
        _lastActionStatus.value = "Interrupted"
    }

    fun triggerPanicStop() {
        isPanicStopped.set(true)
        _lastActionStatus.value = "PANIC STOP TRIGGERED - All operations halted"
    }

    fun resetPanicStop() {
        isPanicStopped.set(false)
    }

    // --- Global Actions ---

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun performPowerDialog(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

    fun performLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun performTakeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    // --- Gestures ---

    fun tap(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (isPanicStopped.get()) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun longPress(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (isPanicStopped.get()) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 700)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (isPanicStopped.get()) return false
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    // --- Node Actions & Content Reading ---

    fun clickByText(targetText: String): Boolean {
        if (isPanicStopped.get()) return false
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            // If parent is clickable
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

    fun clickById(viewId: String): Boolean {
        if (isPanicStopped.get()) return false
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    fun typeText(text: String): Boolean {
        if (isPanicStopped.get()) return false
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        // Try any editable node
        val editable = findFirstEditable(root)
        if (editable != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditable(child)
            if (found != null) return found
        }
        return null
    }

    fun scrollForward(): Boolean {
        if (isPanicStopped.get()) return false
        val root = rootInActiveWindow ?: return false
        return performScroll(root, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollBackward(): Boolean {
        if (isPanicStopped.get()) return false
        val root = rootInActiveWindow ?: return false
        return performScroll(root, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    private fun performScroll(node: AccessibilityNodeInfo, action: Int): Boolean {
        if (node.isScrollable) {
            return node.performAction(action)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (performScroll(child, action)) {
                return true
            }
        }
        return false
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "Screen content unavailable"
        val sb = StringBuilder()
        traverseNode(root, sb, 0)
        return sb.toString().trim()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName

        if (!text.isNullOrEmpty()) {
            sb.append("• ").append(text)
            if (!viewId.isNullOrEmpty()) sb.append(" [id: ").append(viewId.substringAfterLast("/")).append("]")
            sb.append("\n")
        } else if (!desc.isNullOrEmpty()) {
            sb.append("• [icon: ").append(desc).append("]\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, sb, depth + 1)
        }
    }

    fun openApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
