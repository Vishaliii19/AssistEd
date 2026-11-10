package com.assisted.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.assisted.app.databinding.FragmentHomeBinding
import com.assisted.app.managers.TtsManager
import com.assisted.app.managers.VoiceNavManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var ttsManager: TtsManager
    private lateinit var voiceNavManager: VoiceNavManager

    private var commandHandled = false
    private var listeningStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as AssistApp
        ttsManager = app.ttsManager
        voiceNavManager = app.voiceNavManager
        setupClickListeners()
    }

    override fun onPause() {
        super.onPause()
        stopVoiceNav()
    }

    private fun setupClickListeners() {
        binding.cvVoiceNav.setOnClickListener {
            if (voiceNavManager.isListening) {
                stopVoiceNav()
            } else {
                startVoiceNav()
            }
        }

        binding.cvLiveCaption.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CaptionFragment())
        }

        binding.cvScanText.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ScanFragment())
        }

        binding.cvMyNotes.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotesFragment())
        }

        binding.cvEmergencySos.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SosFragment())
        }

        binding.cvSettings.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SettingsFragment())
        }
    }

    private fun startVoiceNav() {
        commandHandled = false
        listeningStarted = false
        binding.tvStatus.text = "Starting… please wait"

        ttsManager.speak("Say a command")
        mainHandler.postDelayed({
            if (isAdded) {
                commandHandled = false
                listeningStarted = true
                binding.tvStatus.text = "Listening… say: caption, scan, notes, help, or settings"

                voiceNavManager.startListening(
                    onResult = { command ->
                        if (!commandHandled && listeningStarted) {
                            commandHandled = true
                            listeningStarted = false
                            handleVoiceCommand(command.lowercase().trim())
                        }
                    },
                    onError = { err ->
                        activity?.runOnUiThread {
                            if (isAdded) {
                                binding.tvStatus.text = "Mic error: $err"
                                listeningStarted = false
                            }
                        }
                    }
                )
            }
        }, 1500)
    }

    private fun stopVoiceNav() {
        voiceNavManager.stopListening()
        listeningStarted = false
        commandHandled = false
        if (isAdded) {
            binding.tvStatus.text = "Ready"
        }
    }

    private fun handleVoiceCommand(command: String) {
        if (command.isBlank()) {
            binding.tvStatus.text = "Heard nothing — try again"
            ttsManager.speak("I didn't hear anything. Please try again.")
            return
        }

        binding.tvOutput.text = "Heard: \"$command\""
        binding.tvStatus.text = "Ready"

        val norm = command.lowercase().trim()

        when {
            norm.contains("caption") || norm.contains("live") || norm.contains("subtitle") || norm.contains("read voice") -> {
                ttsManager.speak("Opening Live Caption")
                binding.cvLiveCaption.performClick()
            }

            norm.contains("scan") || norm.contains("ocr") || norm.contains("text") || norm.contains("read book") -> {
                ttsManager.speak("Opening Text Scanner")
                binding.cvScanText.performClick()
            }

            norm.contains("note") || norm.contains("diary") || norm.contains("save") -> {
                ttsManager.speak("Opening My Notes")
                binding.cvMyNotes.performClick()
            }

            norm.contains("sos") || norm.contains("help") || norm.contains("emergency") || norm.contains("danger") -> {
                ttsManager.speak("Triggering Emergency S O S")
                binding.cvEmergencySos.performClick()
            }

            norm.contains("setting") || norm.contains("config") || norm.contains("option") -> {
                ttsManager.speak("Opening Settings")
                binding.cvSettings.performClick()
            }

            else -> {
                binding.tvStatus.text = "\"$command\" not recognized"
                ttsManager.speak("I heard $command, but I don't know that command. Try saying: caption, scan, notes, or help.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainHandler.removeCallbacksAndMessages(null)
        voiceNavManager.stopListening()
        _binding = null
    }
}
