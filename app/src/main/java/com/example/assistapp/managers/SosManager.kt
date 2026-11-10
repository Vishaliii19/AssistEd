package com.assisted.app.managers

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

class SosManager(private val context: Context) {

    fun triggerSos(phoneNumber: String, studentName: String) {
        vibrateSos()
        sendSms(phoneNumber, studentName)
    }

    private fun vibrateSos() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000) // 3 long vibrations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun sendSms(phoneNumber: String, studentName: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "SOS contact number not configured", Toast.LENGTH_LONG).show()
            return
        }

        // Clean and format the phone number properly for the cellular radio
        var cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
        if (!cleanNumber.startsWith("+")) {
            cleanNumber = if (cleanNumber.length == 10) "+91$cleanNumber" else "+$cleanNumber"
        }

        val message = "AssistEd SOS Alert: $studentName needs help in classroom."

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Create a PendingIntent to track if the SMS was actually sent by the radio
            val sentIntentAction = "SMS_SENT_ACTION_${System.currentTimeMillis()}"
            val sentIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                Intent(sentIntentAction), 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Register the receiver dynamically to catch the result
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (resultCode) {
                        Activity.RESULT_OK -> Toast.makeText(context, "SMS successfully sent via cell network", Toast.LENGTH_LONG).show()
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(context, "SMS Failed: Generic network failure", Toast.LENGTH_LONG).show()
                        SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(context, "SMS Failed: No cell service", Toast.LENGTH_LONG).show()
                        SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(context, "SMS Failed: Null PDU", Toast.LENGTH_LONG).show()
                        SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(context, "SMS Failed: Radio off/Airplane mode", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(context, "SMS Failed: Unknown error code $resultCode", Toast.LENGTH_LONG).show()
                    }
                    // Unregister to avoid memory leaks
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            ContextCompat.registerReceiver(context, receiver, IntentFilter(sentIntentAction), ContextCompat.RECEIVER_NOT_EXPORTED)

            smsManager.sendTextMessage(cleanNumber, null, message, sentIntent, null)
            Toast.makeText(context, "Attempting to send SOS offline to $cleanNumber...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to initiate SMS. Check permissions.", Toast.LENGTH_LONG).show()
        }
    }
}
