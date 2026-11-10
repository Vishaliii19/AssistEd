package com.assisted.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.assisted.app.databinding.FragmentSosBinding
import com.assisted.app.managers.SosManager
import com.assisted.app.managers.TtsManager

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sosManager: SosManager
    private lateinit var ttsManager: TtsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as AssistApp
        ttsManager = app.ttsManager
        sosManager = SosManager(requireContext())

        binding.btnStopSos.text = "SEND EMERGENCY SOS"
        binding.btnStopSos.setOnClickListener {
            sendEmergencySos()
        }
    }

    private fun sendEmergencySos() {
        try {
            val prefs = requireActivity().getSharedPreferences("assist_prefs", Context.MODE_PRIVATE)
            val phoneNumber = prefs.getString("sos_contact", "") ?: ""
            val studentName = prefs.getString("student_name", "Student") ?: "Student"

            if (phoneNumber.isBlank()) {
                binding.tvSosMessage.text = "SOS number not set! Go to Settings."
                ttsManager.speak("SOS number not configured.")
                return
            }

            binding.tvSosMessage.text = "Sending SOS Alert..."
            sosManager.triggerSos(phoneNumber, studentName)
            
            // SosManager handles its own toasts and vibration
            binding.tvSosMessage.text = "Emergency Alert Triggered!"
            ttsManager.speak("SOS Alert Triggered.")
            
        } catch (e: Exception) {
            Log.e("SosFragment", "SOS error", e)
            binding.tvSosMessage.text = "Error: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
