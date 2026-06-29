package com.example.reloader.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AutomationStep
import com.example.reloader.model.StepType
import com.example.reloader.repository.StockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StkAccessibilityService : AccessibilityService() {
    private val TAG = "StkAccessibilityService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var controller: AutomationController
    private val repository = StockRepository.getInstance()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")
        controller = AutomationController(this)
        repository.setAccessibilityEnabled(true)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We handle automation manually, but we can listen for events if needed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        repository.setAccessibilityEnabled(false)
        instance = null
        return super.onUnbind(intent)
    }

    fun startStockCheck() {
        serviceScope.launch {
            Log.d(TAG, "Starting Stock Check Automation")
            repository.updateStatus(AppStatus.Running)

            // Step 1: Launch STK app
            val stkPackages = listOf("com.android.stk", "com.mediatek.stk", "com.android.stk2", "com.qualcomm.qti.services.stk")
            var intent: Intent? = null
            for (pkg in stkPackages) {
                intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) break
            }

            if (intent == null) {
                Log.e(TAG, "STK App not found")
                repository.updateStatus(AppStatus.Failed("SIM Toolkit not found"))
                return@launch
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            // Step 2: Define steps (Customize these based on the actual STK menu)
            // Example flow for Dialog SIM (Sri Lanka) or similar
            val flow = listOf(
                AutomationStep(StepType.WAIT_FOR_TEXT, "eZ Reload", 10000),
                AutomationStep(StepType.CLICK_TEXT, "eZ Reload"),
                AutomationStep(StepType.WAIT_FOR_TEXT, "Options", 5000),
                AutomationStep(StepType.CLICK_TEXT, "Options"),
                AutomationStep(StepType.WAIT_FOR_TEXT, "Inventory", 5000),
                AutomationStep(StepType.CLICK_TEXT, "Inventory"),
                AutomationStep(StepType.WAIT_FOR_TEXT, "Check Stock", 5000),
                AutomationStep(StepType.CLICK_TEXT, "Check Stock"),
                AutomationStep(StepType.WAIT_FOR_INPUT, "", 5000),
                AutomationStep(StepType.INPUT_TEXT, "1111"),
                AutomationStep(StepType.WAIT_FOR_TEXT, "OK", 5000, optional = true),
                AutomationStep(StepType.CLICK_TEXT, "OK", optional = true),
                AutomationStep(StepType.WAIT, "2000"), // Wait for SMS to be sent
                AutomationStep(StepType.HOME)
            )

            val result = controller.runFlow(flow)
            if (result) {
                Log.d(TAG, "Automation flow completed successfully")
                repository.updateStatus(AppStatus.WaitingForSms)
            } else {
                Log.e(TAG, "Automation flow failed")
                repository.updateStatus(AppStatus.Failed("Stock Check Failed"))
            }
        }
    }

    companion object {
        private var instance: StkAccessibilityService? = null
        fun getInstance(): StkAccessibilityService? = instance
    }
}
