package com.example.handwrittingtranslator

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.handwrittingtranslator.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.nio.ByteBuffer


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity()
{
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null


    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tvCountDown: TextView
    private lateinit var tvTranslation: TextView


    // When using Chinese script library
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted())
            startCamera()
        else
            requestPermissions()

        cameraExecutor = Executors.newSingleThreadExecutor()
        tvCountDown = viewBinding.tvCountdown
        tvTranslation = viewBinding.tvTranslation
    }

    private fun processFrame(image: ImageProxy)
    {
        // Convert the ImageProxy to InputImage
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)

        // Process the image with the text recognizer
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                // Process the recognized text
                val recognizedText = visionText.text
                Log.e("debug_tag", recognizedText)

                // Translate the recognized text to the desired language
                translateText(recognizedText, "PT") { translatedText ->
                    // Display the original and translated text on the screen
                    displayTextOnScreen(recognizedText, translatedText)
                }
            }
            .addOnFailureListener { e ->
                Log.e("debug_tag", "Text recognition failed: ${e.message}", e)
            }
            .addOnCompleteListener {
                image.close()
            }
    }

    private fun translateText(text: String, targetLanguage: String, callback: (String) -> Unit)
    {
        // Implement translation logic using a translation service (e.g., Google Translate API)
        // This part depends on the translation service you choose to use
    }

    private fun displayTextOnScreen(originalText: String, translatedText: String)
    {
        // Update UI to display original and translated text
    }


    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Set up the preview use case
            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Set up the image analysis use case
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                        // Process each frame from the camera preview
                        Log.d("debug_tag", "Passing frame")
                        processFrame(image)
                    })
                }

            // Select back camera as the default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try
            {
                // Unbind any existing use cases before binding new ones
                cameraProvider.unbindAll()

                // Bind the preview and image analysis use cases to the camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            }
            catch (exc: Exception)
            {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun requestPermissions()
    {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun processImage(image: InputImage)
    {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                // Handle text recognition results here
                val resultText = visionText.text
//                Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
                Log.i("debug_tag", resultText)
                // Do something with the recognized text
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // Handle error
            }
    }


    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted)
            {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else
            {
                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object
    {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}