package com.gmail.techsavvysanyam.detectify.facedetect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.appSettingOpen
import com.gmail.techsavvysanyam.detectify.databinding.ActivityFaceDetectBinding
import com.gmail.techsavvysanyam.detectify.warningPermissionDialog
import kotlin.math.abs

class FaceDetectActivity : AppCompatActivity(), FaceDetectAnalyzer.FaceResultCallback {
    private val mainBinding: ActivityFaceDetectBinding by lazy {
        ActivityFaceDetectBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = FaceDetectAnalyzer(this)
    private lateinit var imageAnalysis: ImageAnalysis

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayListOf(android.Manifest.permission.CAMERA)
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        setStatusBarColor(R.color.status_bar_blue)
        if (checkMultiplePermission()) {
            startCamera()
        }
        mainBinding.flipCameraIB.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCases()
        }
        mainBinding.FaceScanIB.setOnClickListener {
            scanFace()
        }
        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }
        val copyFaceResult = mainBinding.copyFaceResult
        copyFaceResult.setOnClickListener {
            val allFaceResults = mainBinding.allFaceResults
            val resultText = StringBuilder()

            for (i in 0 until allFaceResults.childCount) {
                val view = allFaceResults.getChildAt(i)
                if (view is TextView) {
                    resultText.append(view.text).append("\n")
                }
            }

            if (resultText.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Face Analysis", resultText.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Face analysis copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No result to copy", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // status bar color
    private fun setStatusBarColor(colorResId: Int) {
        window.statusBarColor = resources.getColor(colorResId, theme)
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionNeeded.toTypedArray(), multiplePermissionId)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_DENIED
                }
                val permanentlyDenied = deniedPermissions.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
                if (permanentlyDenied) {
                    appSettingOpen(this)
                } else {
                    warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            checkMultiplePermission()
                        }
                    }
                }
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun bindCameraUserCases() {
        val screenAspectRatio = aspectRatio(mainBinding.previewFace.width, mainBinding.previewFace.height)
//        val rotation = mainBinding.previewFace.display.rotation
        val rotation = mainBinding.previewFace.display?.rotation ?: Surface.ROTATION_0
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(screenAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(mainBinding.previewFace.surfaceProvider)
            }

        imageAnalysis = ImageAnalysis.Builder()
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun setFlashIcon(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
            } else {
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
            }
        } else {
            Toast.makeText(this, "Flash not supported", Toast.LENGTH_SHORT).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }

    private fun scanFace() {
        imageAnalyzer.setShouldAnalyze(true)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
    }

    override fun onFaceDetected(faces: String?) {
        runOnUiThread {
            mainBinding.faceResultTextView.text = faces
        }
    }
    fun onFaceCountChanged(count: String) {
        runOnUiThread {
            mainBinding.faceCount.text = count
        }
    }
    fun onFaceSmileChanged(smile: String) {
        runOnUiThread {
            mainBinding.smilingProbability.text = smile
        }
    }
    fun onFaceLeftEyeChanged(leftEye: String) {
        runOnUiThread {
            mainBinding.leftEyeOpenProbability.text = leftEye
        }
    }
    fun onFaceRightEyeChanged(rightEye: String) {
        runOnUiThread {
            mainBinding.rightEyeOpenProbability.text = rightEye
        }
    }
}