package com.example.reloader.model

sealed class AppStatus {
    object Idle : AppStatus()
    object Running : AppStatus()
    object WaitingForSms : AppStatus()
    data class Success(val message: String) : AppStatus()
    data class Failed(val error: String) : AppStatus()
}

data class AppUiState(
    val stockInfo: StockInfo = StockInfo(),
    val reloads: List<ReloadAmount> = emptyList(),
    val status: AppStatus = AppStatus.Idle,
    val isAccessibilityEnabled: Boolean = false
)
