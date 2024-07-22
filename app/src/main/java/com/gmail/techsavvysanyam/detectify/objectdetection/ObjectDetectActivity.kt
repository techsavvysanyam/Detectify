package com.gmail.techsavvysanyam.detectify.objectdetection

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityObjectDetectBinding
import com.gmail.techsavvysanyam.detectify.util.CommonButtonUtility
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.gmail.techsavvysanyam.detectify.util.ScreenshotUtility

class ObjectDetectActivity : AppCompatActivity(), ObjectAnalyzer.ObjectResultCallback {
    private val mainBinding: ActivityObjectDetectBinding by lazy {
        ActivityObjectDetectBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = ObjectAnalyzer(this)
    private lateinit var buttonUtility: CommonButtonUtility

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayListOf(android.Manifest.permission.CAMERA)
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        setStatusBarColor(R.color.status_bar_blue)
        if (PermissionUtility.checkMultiplePermission(this, multiplePermissionNameList.toTypedArray(), multiplePermissionId)) {
            initializeButtonUtility()
        }

        mainBinding.ObjectScanIB.setOnClickListener {
            scanImageLabel()
        }

        mainBinding.root.findViewById<View>(R.id.screenshotButton).setOnClickListener {
            ScreenshotUtility.captureScreenshot(
                this,
                mainBinding.root,
                mainBinding.previewObject.getChildAt(0) as SurfaceView,
                mainBinding.allObjectResults ) {}
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
            initializeButtonUtility()
        }
    }
    private fun initializeButtonUtility() {
        buttonUtility = CommonButtonUtility(
            activity = this,
            previewView = mainBinding.previewObject,
            restartButton = mainBinding.root.findViewById(R.id.restartButton),
            flashButton = mainBinding.root.findViewById(R.id.flashToggleIB),
            flipCameraButton = mainBinding.root.findViewById(R.id.flipCameraIB),
            camera = null,
            lensFacing = CameraSelector.LENS_FACING_BACK
        )
    }
    private fun scanImageLabel() {
        imageAnalyzer.setShouldAnalyze(true)
        buttonUtility.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
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