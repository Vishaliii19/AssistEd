package com.assisted.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.assisted.app.data.NoteEntity
import com.assisted.app.databinding.FragmentScanBinding
import com.assisted.app.managers.TtsManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var ttsManager: TtsManager
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastScannedText = ""
    private var isAnalysisActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as AssistApp
        ttsManager = app.ttsManager
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCapture.setOnClickListener {
            isAnalysisActive = true
            binding.tvScanResult.text = "Scanning..."
            Toast.makeText(context, "Scanning text...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveNote.setOnClickListener {
            saveScan()
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isAnalysisActive) {
                            processImageProxy(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("ScanFragment", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank()) {
                        isAnalysisActive = false
                        lastScannedText = text
                        activity?.runOnUiThread {
                            if (isAdded) {
                                binding.tvScanResult.text = text
                                binding.btnSaveNote.isEnabled = true
                                // Read the actual scanned text
                                ttsManager.speak(text)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun saveScan() {
        val textToSave = binding.tvScanResult.text.toString()
        if (textToSave.isNotBlank() && textToSave != "Scanning..." && textToSave != "Point camera at text and tap Scan") {
            val note = NoteEntity(
                text = textToSave,
                timestamp = System.currentTimeMillis(),
                subjectTag = "Scan"
            )
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val app = requireActivity().application as AssistApp
                    app.repository.insert(note)
                    ttsManager.speak("Saved to notes.")
                    Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } catch (e: Exception) {
                    Log.e("ScanFragment", "Save error", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.stop() // Stop reading if user leaves
        cameraExecutor.shutdown()
        _binding = null
    }
}
