package com.example.handwrittingtranslator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {
    // When using Chinese script library
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load image from drawable
        val imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.notes) // Replace `your_image` with the name of your image file

        // Convert Bitmap to InputImage
        val inputImage = InputImage.fromBitmap(imageBitmap, 0)

        // Process the image
        processImage(inputImage)
    }

    private fun processImage(image: InputImage) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                // Handle text recognition results here
                val resultText = visionText.text
                Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
                Log.i("debug_tag", resultText)
                // Do something with the recognized text
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // Handle error
            }
    }
}
