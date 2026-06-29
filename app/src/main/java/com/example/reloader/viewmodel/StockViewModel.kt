package com.example.reloader.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reloader.accessibility.StkAccessibilityService
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.repository.StockRepository
import com.example.reloader.utils.AccessibilityUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository.getInstance()
    val uiState: StateFlow<AppUiState> = repository.uiState

    init {
        checkAccessibilityStatus()
        loadLatestSmsBalance()
    }

    fun checkAccessibilityStatus() {
        val isEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
        repository.setAccessibilityEnabled(isEnabled)
    }

    fun loadLatestSmsBalance() {
        viewModelScope.launch {
            try {
                val uri = Uri.parse("content://sms/inbox")
                val cursor = getApplication<Application>().contentResolver.query(
                    uri,
                    arrayOf("body", "address", "date"),
                    null,
                    null,
                    "date DESC"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val body = it.getString(0)
                        val address = it.getString(1)
                        
                        if (body.contains("RD Balance", ignoreCase = true)) {
                            parseAndUpdateFromSms(body)
                            break // Found the latest one
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error reading SMS inbox", e)
            }
        }
    }

    private fun parseAndUpdateFromSms(body: String) {
        val balancePattern = Pattern.compile("RD Balance:\\s*Rs\\.?\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balancePattern.matcher(body)
        val balance = if (balanceMatcher.find()) "Rs. ${balanceMatcher.group(1)}" else null

        val commissionPattern = Pattern.compile("Commission Balance:\\s*Rs\\.?\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE)
        val commissionMatcher = commissionPattern.matcher(body)
        val commission = if (commissionMatcher.find()) "Rs. ${commissionMatcher.group(1)}" else "Rs. 0.00"

        if (balance != null) {
            repository.updateStockInfo(balance, commission)
            repository.updateStatus(AppStatus.Idle)
        }
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
