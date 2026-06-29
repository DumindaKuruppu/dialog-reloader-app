package com.example.reloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reloader.accessibility.StkAccessibilityService
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.repository.StockRepository
import com.example.reloader.utils.AccessibilityUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository.getInstance()
    val uiState: StateFlow<AppUiState> = repository.uiState

    init {
        checkAccessibilityStatus()
    }

    fun checkAccessibilityStatus() {
        val isEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
        repository.setAccessibilityEnabled(isEnabled)
    }

    fun checkStock() {
        if (!uiState.value.isAccessibilityEnabled) {
            repository.updateStatus(AppStatus.Failed("Accessibility Service Disabled"))
            return
        }

        val service = StkAccessibilityService.getInstance()
        if (service != null) {
            service.startStockCheck()
        } else {
            repository.updateStatus(AppStatus.Failed("Service not running. Please enable it."))
        }
    }

    fun resetStatus() {
        repository.updateStatus(AppStatus.Idle)
    }
}
