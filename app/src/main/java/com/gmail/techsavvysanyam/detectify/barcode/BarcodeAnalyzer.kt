package com.gmail.techsavvysanyam.detectify.barcode

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val context: Context, private val callback: BarcodeActivity) : ImageAnalysis.Analyzer {
    private var shouldAnalyze = false

    private val scanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
//        Log.d("BARCODE","image analysed")
//        Toast.makeText(context, "Image analysed", Toast.LENGTH_SHORT).show()
        if (!shouldAnalyze) {
            imageProxy.close()
            return
        }
        imageProxy.image?.let {
            val inputImage = InputImage.fromMediaImage(
                it,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener {codes ->
                    codes.forEach { barcode ->
                        val rawValue = barcode.rawValue
                        callback.onBarcodeDetected(rawValue)                    }
                }
                .addOnFailureListener { ex ->
                    Log.e("BARCODE","barcode detection failed", ex)
                    Toast.makeText(context, "Barcode detection failed", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?:imageProxy.close()
    }
    fun setShouldAnalyze(value: Boolean) {
        shouldAnalyze = value
    }
    interface BarcodeResultCallback {
        fun onBarcodeDetected(barcode: String?)
    }
}
