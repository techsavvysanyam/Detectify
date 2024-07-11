package com.gmail.techsavvysanyam.detectify.textrecognition

import android.content.ContentValues.TAG
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRgAnalyzer(private val callback: TextRgActivity) : ImageAnalysis.Analyzer {

    private var shouldAnalyze = false

    fun setShouldAnalyze(shouldAnalyze: Boolean) {
        this.shouldAnalyze = shouldAnalyze
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!shouldAnalyze) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.textBlocks.joinToString("\n") { it.text }
                    callback.onTextRgLine(recognizedText)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    interface TextRgResultCallback {
        fun onTextRgLine(text: String?)
    }
}
