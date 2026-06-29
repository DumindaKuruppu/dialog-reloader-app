package com.example.reloader.repository

import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.model.StockInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockRepository {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun updateStatus(status: AppStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun updateStockInfo(balance: String, commission: String) {
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        _uiState.update {
            it.copy(
                stockInfo = StockInfo(balance, commission, currentTime),
                status = AppStatus.Success("Stock Updated")
            )
        }
    }

    fun setAccessibilityEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
    }

    companion object {
        @Volatile
        private var instance: StockRepository? = null

        fun getInstance(): StockRepository {
            return instance ?: synchronized(this) {
                instance ?: StockRepository().also { instance = it }
            }
        }
    }
}
