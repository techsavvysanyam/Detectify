package com.gmail.techsavvysanyam.detectify.objectdetection

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityObjectDetectBinding
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.gmail.techsavvysanyam.detectify.util.ScreenshotUtility
import kotlin.math.abs

class ObjectDetectActivity : AppCompatActivity(), ObjectAnalyzer.ObjectResultCallback {
    private val mainBinding: ActivityObjectDetectBinding by lazy {
        ActivityObjectDetectBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = ObjectAnalyzer(this)
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
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        setStatusBarColor(R.color.status_bar_blue)
        if (PermissionUtility.checkMultiplePermission(this, multiplePermissionNameList.toTypedArray(), multiplePermissionId)) {
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
        mainBinding.ObjectScanIB.setOnClickListener {
            scanImageLabel()
        }
        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }
        mainBinding.screenshotButton.setOnClickListener {
            ScreenshotUtility.captureScreenshot(
                this,
                mainBinding.root,
                mainBinding.previewObject.getChildAt(0) as SurfaceView,
                mainBinding.allObjectResults
            ) {
                Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
            }
        }

        val copyObjectR = mainBinding.copyObjectR
        copyObjectR.setOnClickListener {
            val objectText = mainBinding.objectResultTextView.text
            val objectConfidence = mainBinding.objectConfidence.text

            if (!objectText.isNullOrEmpty() || !objectConfidence.isNullOrEmpty()) {
                val combinedText = "Object: $objectText\n$objectConfidence"
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Object and Confidence", combinedText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Object and confidence copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No object or confidence to copy", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // status bar color
    private fun setStatusBarColor(colorResId: Int) {
        window.statusBarColor = resources.getColor(colorResId, theme)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtility.handlePermissionResult(
            this,
            requestCode,
            permissions,
            grantResults,
            multiplePermissionId
        ) {
            startCamera()
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
        val screenAspectRatio =
            aspectRatio(mainBinding.previewObject.width, mainBinding.previewObject.height)
        val rotation = mainBinding.previewObject.display?.rotation ?: Surface.ROTATION_0
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
                it.setSurfaceProvider(mainBinding.previewObject.surfaceProvider)
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

    private fun scanImageLabel() {
        imageAnalyzer.setShouldAnalyze(true)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
    }

    override fun onObjectDetected(labels: String?) {
        runOnUiThread {
            mainBinding.objectResultTextView.text = labels
        }
    }
    @SuppressLint("SetTextI18n")
    fun onObjectCheckConfidence(confidence: String?) {
        runOnUiThread{
            mainBinding.objectConfidence.text = "Confidence: $confidence"
        }
    }
    @SuppressLint("SetTextI18n")
    fun onObjectTrackingId(trackingId: String?) {
        runOnUiThread {
            mainBinding.objectTrackingId.text = "Tracking ID: $trackingId"
        }
    }
}