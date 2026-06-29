package com.example.reloader.model

sealed class AppStatus {
    object Idle : AppStatus()
    object Running : AppStatus()
    object WaitingForSms : AppStatus()
    data class Success(val message: String) : AppStatus()
    data class Failed(val error: String) : AppStatus()
}

data class PresetAmount(
    val amount: String,
    val description: String = ""
)

data class AppUiState(
    val stockInfo: StockInfo = StockInfo(),
    val reloads: List<ReloadAmount> = emptyList(),
    val presetAmounts: List<PresetAmount> = listOf(
        PresetAmount("50"),
        PresetAmount("100"),
        PresetAmount("200"),
        PresetAmount("500"),
        PresetAmount("1000")
    ),
    val recentMessages: List<SmsMessage> = emptyList(),
    val status: AppStatus = AppStatus.Idle,
    val isAccessibilityEnabled: Boolean = false
)

data class SmsMessage(
    val body: String,
    val date: String
)
