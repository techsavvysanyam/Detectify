package com.gmail.techsavvysanyam.detectify.facedetect

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectAnalyzer(private val callback: FaceDetectActivity) : ImageAnalysis.Analyzer {
    private var shouldAnalyze = false

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

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

            detector.process(inputImage)
                .addOnSuccessListener {faces ->
                    val count = faces.size
                    callback.onFaceCountChanged("Number of faces: $count")
                    faces.forEach { face ->
                        val confidence = face.smilingProbability
                        val leftEyeOpenProbability = face.leftEyeOpenProbability
                        val rightEyeOpenProbability = face.rightEyeOpenProbability


                        callback.onFaceSmileChanged("Smiling Probability: ${confidence?.times(100)?.let { "%.2f".format(it) }}%")
                        callback.onFaceLeftEyeChanged("Right Eye Open Probability: ${leftEyeOpenProbability?.times(100)?.let { "%.2f".format(it) }}%")
                        callback.onFaceRightEyeChanged("Left Eye Open Probability: ${rightEyeOpenProbability?.times(100)?.let { "%.2f".format(it) }}%")
                     }
                }

                .addOnFailureListener { ex ->
                    Log.e("FACE-DETECT","barcode detection failed", ex)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?:imageProxy.close()
    }
    fun setShouldAnalyze(value: Boolean) {
        shouldAnalyze = value
    }
        interface FaceResultCallback {
            fun onFaceDetected(faces: String?)
        }
}