package com.gmail.techsavvysanyam.detectify.objectdetection

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class ObjectAnalyzer(private val callback: ObjectDetectActivity) : ImageAnalysis.Analyzer {
    private var shouldAnalyze = false

    private val localModel = LocalModel.Builder()
        .setAssetFilePath("object_labeler.tflite")
        .build()

    private val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.7f)
            .setMaxPerObjectLabelCount(3)
            .build()

    private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
//        Log.d("FACE-DETECT","image analysed")
//        Toast.makeText(context, "Image analysed", Toast.LENGTH_SHORT).show()
        if (!shouldAnalyze) {
            imageProxy.close()
            return
        }
        imageProxy.image?.let { it ->
            val inputImage = InputImage.fromMediaImage(
                it,
                imageProxy.imageInfo.rotationDegrees
            )

            objectDetector.process(inputImage)
                .addOnFailureListener { ex ->
                    Log.e("OBJECT-DETECT","Object detection failed", ex)
                }
                .addOnSuccessListener { results ->
                    for (detectedObject in results) {
                        val trackingId = detectedObject.trackingId
                        callback.onObjectTrackingId(trackingId.toString())
                        for (label in detectedObject.labels) {
                            val text = label.text
                            val confidence = label.confidence
                            callback.onObjectDetected(text)
                            callback.onObjectCheckConfidence("${confidence.times(100).let { "%.2f".format(it) }}%")
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?:imageProxy.close()
    }
    fun setShouldAnalyze(value: Boolean) {
        shouldAnalyze = value
    }
    interface ObjectResultCallback {
        fun onObjectDetected(labels: String?)
    }
}