package com.example.reloader.model

enum class StepType {
    WAIT_FOR_TEXT,
    CLICK_TEXT,
    CLICK_ID,
    WAIT,
    BACK,
    HOME,
    WAIT_FOR_INPUT,
    INPUT_TEXT,
    END_SESSION
}

data class AutomationStep(
    val type: StepType,
    val value: String? = null,
    val timeoutMs: Long = 5000,
    val optional: Boolean = false
)
