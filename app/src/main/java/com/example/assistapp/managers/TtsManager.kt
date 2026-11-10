package com.assisted.app.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    // Keep application context to avoid memory leaks
    private val appContext = context.applicationContext

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.w("TtsManager", "US English not supported, using default")
                            tts?.setLanguage(Locale.getDefault())
                        }
                        isInitialized = true
                        Log.d("TtsManager", "TTS initialized successfully")
                    } else {
                        Log.e("TtsManager", "TTS init failed with status: $status")
                    }
                }
            } catch (e: Exception) {
                Log.e("TtsManager", "TTS constructor failed", e)
            }
        }
    }

    fun setSpeed(speed: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(speed)
        }
    }

    fun setLanguage(locale: Locale) {
        if (isInitialized) {
            tts?.setLanguage(locale)
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (text.isBlank()) return
        mainHandler.post {
            if (isInitialized && tts != null) {
                val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(text, queueMode, null, "utterance_${System.currentTimeMillis()}")
            } else {
                Log.w("TtsManager", "speak() called but TTS not ready")
            }
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e("TtsManager", "TTS stop error", e)
            }
        }
    }

    fun shutdown() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
            } catch (e: Exception) {
                Log.e("TtsManager", "TTS shutdown error", e)
            }
        }
    }
}
