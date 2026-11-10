package com.assisted.app.managers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * VoiceNavManager: wraps Android SpeechRecognizer.
 * ALL SpeechRecognizer operations MUST run on the main thread.
 */
class VoiceNavManager(context: Context) {

    private val appContext = context // Use provided context directly
    private val mainHandler = Handler(Looper.getMainLooper())

    // Only access on main thread
    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile var isListening = false
        private set

    // Stored callbacks for continuous mode
    private var currentOnResult: ((String) -> Unit)? = null
    private var currentOnPartial: ((String) -> Unit)? = null
    private var currentOnError: ((String) -> Unit)? = null
    private var isContinuous = false

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit = {},
        onError: (String) -> Unit = {},
        continuous: Boolean = false
    ) {
        // Must run on main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            doStartListening(onResult, onPartialResult, onError, continuous)
        } else {
            mainHandler.post {
                doStartListening(onResult, onPartialResult, onError, continuous)
            }
        }
    }

    private fun doStartListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        continuous: Boolean
    ) {
        // We are guaranteed to be on main thread here
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                Log.e("VoiceNavManager", "Speech recognition NOT available")
                onError("Speech recognition not available")
                return
            }

            if (androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.RECORD_AUDIO) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("VoiceNavManager", "RECORD_AUDIO permission NOT granted")
                onError("Microphone permission missing")
                return
            }

            isContinuous = continuous
            currentOnResult = onResult
            currentOnPartial = onPartialResult
            currentOnError = onError

            // CLEANUP previous instance properly
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceNavManager", "Cleanup error", e)
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    Log.d("VoiceNavManager", "Ready for speech - Microphone is now OPEN")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceNavManager", "Speech begun")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    Log.d("VoiceNavManager", "End of speech")
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Error code: $error"
                    }
                    Log.e("VoiceNavManager", "Recognition error: $message")
                    
                    // Show a toast so user knows what's happening
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        mainHandler.post {
                            android.widget.Toast.makeText(appContext, "Voice Error: $message", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    currentOnError?.invoke(message)

                    // Auto-restart on some recoverable errors in continuous mode
                    if (isContinuous && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        && error != SpeechRecognizer.ERROR_CLIENT) {
                        mainHandler.postDelayed({
                            if (isContinuous) {
                                doStartListening(
                                    currentOnResult ?: {},
                                    currentOnPartial ?: {},
                                    currentOnError ?: {},
                                    true
                                )
                            }
                        }, 1000)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        Log.d("VoiceNavManager", "Result: ${matches[0]}")
                        currentOnResult?.invoke(matches[0])
                    }
                    // Restart for continuous mode
                    if (isContinuous) {
                        mainHandler.postDelayed({
                            if (isContinuous) {
                                doStartListening(
                                    currentOnResult ?: {},
                                    currentOnPartial ?: {},
                                    currentOnError ?: {},
                                    true
                                )
                            }
                        }, 300)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        currentOnPartial?.invoke(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Force English (US) if default fails, or just use English (US) as primary
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                
                // ADDED: Explicitly prefer offline recognition for true offline functionality
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                // Help keep the mic open and responsive
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            }

            speechRecognizer?.startListening(intent)
            Log.d("VoiceNavManager", "startListening called")

        } catch (e: Exception) {
            Log.e("VoiceNavManager", "Failed to start listening", e)
            isListening = false
            onError("Failed to start: ${e.message}")
        }
    }

    fun stopListening() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            doStopListening()
        } else {
            mainHandler.post { doStopListening() }
        }
    }

    private fun doStopListening() {
        isContinuous = false
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("VoiceNavManager", "Error stopping recognizer", e)
        }
        currentOnResult = null
        currentOnPartial = null
        currentOnError = null
        Log.d("VoiceNavManager", "Stopped listening")
    }
}
