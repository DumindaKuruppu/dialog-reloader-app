package com.example.reloader.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.reloader.repository.StockRepository
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    private val repository = StockRepository.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.displayMessageBody
                val sender = message.displayOriginatingAddress

                Log.d(TAG, "SMS received from: $sender body: $body")

                // Filter by sender if known, e.g., "Dialog" or a short code
                // For demonstration, we check if body contains "Balance" and "Commission"
                if (body.contains("Balance", ignoreCase = true) || body.contains("Commission", ignoreCase = true)) {
                    parseAndUpdateStock(body)
                }
            }
        }
    }

    private fun parseAndUpdateStock(body: String) {
        try {
            // Regex for Balance: Rs. 12,500.00
            val balancePattern = Pattern.compile("Balance\\s*:\\s*Rs\\.\\s*([\\d,]+\\.\\d{2})")
            val balanceMatcher = balancePattern.matcher(body)
            val balance = if (balanceMatcher.find()) "Rs. ${balanceMatcher.group(1)}" else "Rs. 0.00"

            // Regex for Commission: Rs. 860.00
            val commissionPattern = Pattern.compile("Commission\\s*:\\s*Rs\\.\\s*([\\d,]+\\.\\d{2})")
            val commissionMatcher = commissionPattern.matcher(body)
            val commission = if (commissionMatcher.find()) "Rs. ${commissionMatcher.group(1)}" else "Rs. 0.00"

            Log.d(TAG, "Parsed Balance: $balance, Commission: $commission")
            repository.updateStockInfo(balance, commission)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS", e)
        }
    }
}
