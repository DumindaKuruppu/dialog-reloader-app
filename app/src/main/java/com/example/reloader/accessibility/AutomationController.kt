package com.example.reloader.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.reloader.model.AutomationStep
import com.example.reloader.model.StepType
import kotlinx.coroutines.delay

class AutomationController(private val service: AccessibilityService) {
    private val TAG = "AutomationController"

    suspend fun runFlow(steps: List<AutomationStep>): Boolean {
        for (step in steps) {
            Log.d(TAG, "Executing step: ${step.type} with value: ${step.value}")
            val success = when (step.type) {
                StepType.WAIT_FOR_TEXT -> waitForText(step.value, step.timeoutMs)
                StepType.CLICK_TEXT -> clickText(step.value)
                StepType.CLICK_ID -> clickId(step.value)
                StepType.WAIT -> {
                    delay(step.value?.toLong() ?: 1000)
                    true
                }
                StepType.BACK -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    true
                }
                StepType.HOME -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    true
                }
                StepType.WAIT_FOR_INPUT -> waitForInput(step.timeoutMs)
                StepType.INPUT_TEXT -> inputText(step.value)
                StepType.END_SESSION -> endSession()
            }

            if (!success && !step.optional) {
                Log.e(TAG, "Step failed: ${step.type} ${step.value}")
                return false
            }
            delay(500) // Small delay between steps
        }
        return true
    }

    private suspend fun waitForText(text: String?, timeout: Long): Boolean {
        if (text == null) return false
        Log.d(TAG, "Waiting for text: '$text' (Timeout: ${timeout}ms)")
        val startTime = System.currentTimeMillis()
        
        // Give the UI a moment to start transitioning if a click just happened
        delay(300)

        while (System.currentTimeMillis() - startTime < timeout) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                // Filter for nodes that are actually visible and match exactly
                val hasMatch = nodes.any { node ->
                    node.refresh() 
                    val isVisible = node.isVisibleToUser
                    val matches = node.text?.toString()?.equals(text, ignoreCase = true) == true ||
                                 node.contentDescription?.toString()?.equals(text, ignoreCase = true) == true
                    isVisible && matches
                }
                
                nodes.forEach { it.recycle() }
                root.recycle()
                
                if (hasMatch) {
                    Log.d(TAG, "Found visible match for: '$text'")
                    // Wait a tiny bit more to ensure the UI is stable/clickable
                    delay(200)
                    return true
                }
            }
            delay(500)
        }
        Log.e(TAG, "Timed out waiting for text: '$text'")
        return false
    }

    private suspend fun waitForInput(timeout: Long): Boolean {
        Log.d(TAG, "Waiting for input field (Timeout: ${timeout}ms)")
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val inputNode = findInputNode(root)
                if (inputNode != null) {
                    Log.d(TAG, "Found visible input field")
                    inputNode.recycle()
                    root.recycle()
                    return true
                }
                root.recycle()
            }
            delay(500)
        }
        Log.e(TAG, "Timed out waiting for input field")
        return false
    }

    private fun inputText(text: String?): Boolean {
        if (text == null) return false
        val root = service.rootInActiveWindow ?: return false
        val inputNode = findInputNode(root)
        if (inputNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            inputNode.recycle()
            root.recycle()
            return success
        }
        root.recycle()
        return false
    }

    private fun findInputNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if ((root.isEditable || root.className?.contains("EditText", ignoreCase = true) == true) && root.isVisibleToUser) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findInputNode(child)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }

    fun clickText(text: String?): Boolean {
        if (text == null) return false
        val root = service.rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (performClick(node)) {
                nodes.forEach { it.recycle() }
                root.recycle()
                return true
            }
        }
        nodes.forEach { it.recycle() }
        root.recycle()
        return false
    }

    fun clickId(id: String?): Boolean {
        if (id == null) return false
        val root = service.rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        for (node in nodes) {
            if (performClick(node)) {
                nodes.forEach { it.recycle() }
                root.recycle()
                return true
            }
        }
        nodes.forEach { it.recycle() }
        root.recycle()
        return false
    }

    fun clickContentDescription(desc: String?): Boolean {
        if (desc == null) return false
        val root = service.rootInActiveWindow ?: return false
        val node = findNodeByContentDescription(root, desc)
        if (node != null) {
            val success = performClick(node)
            node.recycle()
            root.recycle()
            return success
        }
        root.recycle()
        return false
    }

    private fun findNodeByContentDescription(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (root.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true && root.isVisibleToUser) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeByContentDescription(child, desc)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private suspend fun endSession(): Boolean {
        val menuClicked = clickContentDescription("More options") || 
                         clickContentDescription("Menu")
        
        if (menuClicked) {
            delay(1000)
            if (clickText("End Session")) {
                Log.d(TAG, "Session ended successfully")
                return true
            }
        }
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            val parent = current.parent
            if (current != node) current.recycle() // Recycle intermediate parents
            current = parent
        }
        return false
    }
}
