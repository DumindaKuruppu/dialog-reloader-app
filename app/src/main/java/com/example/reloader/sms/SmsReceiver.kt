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
                val sender = message.displayOriginatingAddress ?: ""

                Log.d(TAG, "SMS received from: $sender body: $body")

                // Filter by sender "eZ Reload" or check for specific keywords
                if (sender.contains("eZ Reload", ignoreCase = true) || 
                    body.contains("RD Balance", ignoreCase = true)) {
                    parseAndUpdateStock(body)
                }
            }
        }
    }

    private fun parseAndUpdateStock(body: String) {
        try {
            // Updated Regex for RD Balance: matches "RD Balance: Rs 3196.00" or "RD Balance: Rs. 3196.00"
            val balancePattern = Pattern.compile("RD Balance:\\s*Rs\\.?\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE)
            val balanceMatcher = balancePattern.matcher(body)
            val balance = if (balanceMatcher.find()) "Rs. ${balanceMatcher.group(1)}" else "Rs. 0.00"

            // Updated Regex for Commission Balance
            val commissionPattern = Pattern.compile("Commission Balance:\\s*Rs\\.?\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE)
            val commissionMatcher = commissionPattern.matcher(body)
            val commission = if (commissionMatcher.find()) "Rs. ${commissionMatcher.group(1)}" else "Rs. 0.00"

            Log.d(TAG, "Parsed RD Balance: $balance, Commission: $commission")
            repository.updateStockInfo(balance, commission)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS", e)
        }
    }
}
