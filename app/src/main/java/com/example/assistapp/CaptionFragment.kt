package com.assisted.app

import android.os.Bundle
import java.lang.StringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.assisted.app.databinding.FragmentCaptionBinding
import com.assisted.app.managers.TtsManager
import com.assisted.app.managers.VoiceNavManager

class CaptionFragment : Fragment() {

    private var _binding: FragmentCaptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var voiceNavManager: VoiceNavManager
    private lateinit var ttsManager: TtsManager
    private val fullText = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as AssistApp
        voiceNavManager = app.voiceNavManager
        ttsManager = app.ttsManager

        binding.btnStopCaption.setOnClickListener {
            (activity as? MainActivity)?.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start capturing in onResume to ensure we are the active fragment
        startCapturing()
    }

    override fun onPause() {
        super.onPause()
        voiceNavManager.stopListening()
        ttsManager.stop()
    }

    private fun startCapturing() {
        fullText.clear()
        binding.tvLiveText.text = "Listening... speak now"

        try {
            voiceNavManager.startListening(
                onResult = { result ->
                    activity?.runOnUiThread {
                        if (isAdded) {
                            val trimmed = result.trim()
                            // Simple noise check
                            val isNoise = trimmed.length < 2 || trimmed.equals("uh", true) || trimmed.equals("um", true)
                            
                            if (!isNoise && trimmed.isNotBlank()) {
                                if (trimmed.contains("stop", ignoreCase = true)) {
                                    ttsManager.stop()
                                    binding.tvLiveText.text = fullText.toString() + "\n(Speech stopped by voice)"
                                } else {
                                    fullText.append("$trimmed. ")
                                    binding.tvLiveText.text = fullText.toString()
                                    // Read loud the captured sentence
                                    ttsManager.speak(trimmed)
                                    binding.scrollView.post {
                                        binding.scrollView.fullScroll(View.FOCUS_DOWN)
                                    }
                                }
                            }
                        }
                    }
                },
                onPartialResult = { partial ->
                    activity?.runOnUiThread {
                        if (isAdded && partial.isNotBlank()) {
                            binding.tvLiveText.text = fullText.toString() + partial + "..."
                        }
                    }
                },
                onError = { error ->
                    activity?.runOnUiThread {
                        if (isAdded) {
                            binding.tvLiveText.text = "Error: $error. Reconnecting..."
                        }
                    }
                },
                continuous = true
            )
        } catch (e: Exception) {
            binding.tvLiveText.text = "Initialization Error: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceNavManager.stopListening()
        ttsManager.stop()
        _binding = null
    }
}
