package com.example.handwrittingtranslator

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
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
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.example.handwrittingtranslator.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions


class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var countdownTimer: CountDownTimer? = null
    private var imageCapture: ImageCapture? = null

    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted())
        {
            startCamera()
            startCountdown()
        }
        else
        {
            requestPermissions()
        }
    }

    private fun processFrame(image: ImageProxy)
    {
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                Log.d("debug_tag", "Recognizing text!")
                Log.d("debug_tag", recognizedText)


            }
            .addOnFailureListener { e ->
                Log.e("debug_tag", "Text recognition failed", e)
            }
            .addOnCompleteListener {
                image.close()
            }
    }

    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try
            {
                // Unbind any existing use cases before binding new ones
                cameraProvider.unbindAll()

                // Bind the preview and image capture use cases to the camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }
            catch (exc: Exception)
            {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCountdown()
    {
        countdownTimer = object : CountDownTimer(10000, 1000)
        {
            override fun onTick(millisUntilFinished: Long)
            {
                val secondsUntilNextExtraction = millisUntilFinished / 1000
                binding.tvCountdown.text =
                    "Countdown until next translation: $secondsUntilNextExtraction seconds"
            }

            override fun onFinish()
            {
                // Restart the countdown
                start()
                // Trigger image capture and text recognition here
                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(this@MainActivity),
                    object : ImageCapture.OnImageCapturedCallback()
                    {
                        override fun onCaptureSuccess(image: ImageProxy)
                        {
                            processFrame(image)
                        }

                        override fun onError(exception: ImageCaptureException)
                        {
                            Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                        }
                    })
            }
        }.start()
    }


    private fun requestPermissions()
    {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
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
        private const val FILENAME_FORMAT = "ss-SSS"
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
