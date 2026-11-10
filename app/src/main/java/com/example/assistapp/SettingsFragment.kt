package com.assisted.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.assisted.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("assist_prefs", Context.MODE_PRIVATE)

        // Load saved values
        binding.etStudentName.setText(prefs.getString("student_name", ""))
        binding.etSosContact.setText(prefs.getString("sos_contact", ""))
        binding.switchNoiseFilter.isChecked = prefs.getBoolean("noise_filter", false)

        binding.btnSaveSettings.setOnClickListener {
            try {
                val name = binding.etStudentName.text?.toString()?.trim() ?: ""
                val contact = binding.etSosContact.text?.toString()?.trim() ?: ""

                prefs.edit().apply {
                    putString("student_name", name)
                    putString("sos_contact", contact)
                    putBoolean("noise_filter", binding.switchNoiseFilter.isChecked)
                    apply()
                }
                Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Save error", e)
                Toast.makeText(context, "Failed to save settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
