package com.example.reloader.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reloader.accessibility.StkAccessibilityService
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.model.ReloadAmount
import com.example.reloader.repository.StockRepository
import com.example.reloader.utils.AccessibilityUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository.getInstance()
    val uiState: StateFlow<AppUiState> = repository.uiState

    init {
        checkAccessibilityStatus()
        loadLatestSmsBalance()
        loadSavedReloads()
        loadPresetAmounts()
    }

    private fun loadPresetAmounts() {
        val prefs = getApplication<Application>().getSharedPreferences("settings", Application.MODE_PRIVATE)
        val savedData = prefs.getStringSet("preset_amounts_v2", null)
        
        if (savedData == null) {
            // Migration or initial load
            val oldAmounts = prefs.getStringSet("preset_amounts", setOf("50", "100", "200", "500", "1000"))
            val presets = oldAmounts?.map { com.example.reloader.model.PresetAmount(it) } ?: emptyList()
            repository.updatePresetAmounts(presets)
        } else {
            val presets = savedData.mapNotNull { 
                val parts = it.split("|", limit = 2)
                if (parts.isNotEmpty()) {
                    com.example.reloader.model.PresetAmount(parts[0], if (parts.size > 1) parts[1] else "")
                } else null
            }
            repository.updatePresetAmounts(presets)
        }
    }

    fun addPresetAmount(amount: String, description: String = "") {
        if (amount.isEmpty()) return
        val current = uiState.value.presetAmounts.toMutableList()
        // Replace if exists, or add new
        current.removeAll { it.amount == amount }
        current.add(com.example.reloader.model.PresetAmount(amount, description))
        repository.updatePresetAmounts(current)
        persistPresetAmounts()
    }

    fun deletePresetAmount(amount: String) {
        val current = uiState.value.presetAmounts.toMutableList()
        current.removeAll { it.amount == amount }
        repository.updatePresetAmounts(current)
        persistPresetAmounts()
    }

    private fun persistPresetAmounts() {
        val prefs = getApplication<Application>().getSharedPreferences("settings", Application.MODE_PRIVATE)
        val dataSet = uiState.value.presetAmounts.map { "${it.amount}|${it.description}" }.toSet()
        prefs.edit().putStringSet("preset_amounts_v2", dataSet).apply()
    }

    private fun loadSavedReloads() {
        val prefs = getApplication<Application>().getSharedPreferences("reloads", Application.MODE_PRIVATE)
        val savedData = prefs.getStringSet("reload_list", emptySet()) ?: emptySet()
        val reloads = savedData.mapNotNull { 
            val parts = it.split("|")
            if (parts.size == 4) ReloadAmount(parts[0], parts[1], parts[2], parts[3]) else null
        }
        repository.updateReloads(reloads)
    }

    fun saveReload(label: String, phoneNumber: String, amount: String) {
        val newReload = ReloadAmount(label = label, phoneNumber = phoneNumber, amount = amount)
        repository.addReload(newReload)
        persistReloads()
    }

    fun deleteReload(id: String) {
        repository.removeReload(id)
        persistReloads()
    }

    private fun persistReloads() {
        val prefs = getApplication<Application>().getSharedPreferences("reloads", Application.MODE_PRIVATE)
        val dataSet = uiState.value.reloads.map { "${it.id}|${it.label}|${it.phoneNumber}|${it.amount}" }.toSet()
        prefs.edit().putStringSet("reload_list", dataSet).apply()
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

                val messages = mutableListOf<com.example.reloader.model.SmsMessage>()
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

                cursor?.use {
                    var balanceUpdated = false
                    while (it.moveToNext()) {
                        val body = it.getString(0) ?: ""
                        val address = it.getString(1) ?: ""
                        val dateLong = it.getLong(2)
                        val dateStr = dateFormat.format(Date(dateLong))
                        
                        // Check if message is from eZ Reload or contains relevant text
                        val isEzReloadMsg = address.contains("eZ Reload", ignoreCase = true) || 
                                           address.contains("7111", ignoreCase = true) || // Common eZ Reload shortcode
                                           body.contains("RD Balance", ignoreCase = true) || 
                                           body.contains("Reload Success", ignoreCase = true) ||
                                           body.contains("eZ Reload", ignoreCase = true)
                        
                        if (isEzReloadMsg) {
                            if (!balanceUpdated && body.contains("RD Balance", ignoreCase = true)) {
                                parseAndUpdateFromSms(body)
                                balanceUpdated = true
                            }
                            
                            // Capture more messages (up to 50)
                            if (messages.size < 50) {
                                messages.add(com.example.reloader.model.SmsMessage(body, dateStr))
                            }
                        }
                    }
                }
                repository.updateRecentMessages(messages)
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

    fun performReload(reload: ReloadAmount) {
        performQuickReload(reload.phoneNumber, reload.amount)
    }

    fun performQuickReload(phoneNumber: String, amount: String) {
        if (!uiState.value.isAccessibilityEnabled) {
            repository.updateStatus(AppStatus.Failed("Accessibility Service Disabled"))
            return
        }

        val service = StkAccessibilityService.getInstance()
        if (service != null) {
            service.startReload(phoneNumber, amount)
        } else {
            repository.updateStatus(AppStatus.Failed("Service not running. Please enable it."))
        }
    }

    fun resetStatus() {
        repository.updateStatus(AppStatus.Idle)
    }
}
