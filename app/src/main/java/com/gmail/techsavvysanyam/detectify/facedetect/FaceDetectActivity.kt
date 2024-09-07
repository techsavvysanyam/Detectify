package com.gmail.techsavvysanyam.detectify.facedetect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityFaceDetectBinding
import com.gmail.techsavvysanyam.detectify.util.CommonButtonUtility
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.gmail.techsavvysanyam.detectify.util.ScreenshotUtility

class FaceDetectActivity : AppCompatActivity(), FaceDetectAnalyzer.FaceResultCallback {
    private val mainBinding: ActivityFaceDetectBinding by lazy {
        ActivityFaceDetectBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = FaceDetectAnalyzer(this)
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
        if (PermissionUtility.checkMultiplePermission(this, multiplePermissionNameList.toTypedArray(), multiplePermissionId)) {
            initializeButtonUtility()
        }

        mainBinding.FaceScanIB.setOnClickListener {
            scanFace()
        }
        mainBinding.root.findViewById<View>(R.id.screenshotButton).setOnClickListener {
            ScreenshotUtility.captureScreenshot(
                this,
                mainBinding.root,
                mainBinding.previewFace.getChildAt(0) as SurfaceView,
                mainBinding.allFaceResults
            ) {}
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
            previewView = mainBinding.previewFace,
            restartButton = mainBinding.root.findViewById(R.id.restartButton),
            flashButton = mainBinding.root.findViewById(R.id.flashToggleIB),
            camera = null,
            flipCameraButton = mainBinding.root.findViewById(R.id.flipCameraIB),
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            toggleButton = mainBinding.root.findViewById(R.id.frameToggleButton),
            topOverlay = mainBinding.root.findViewById(R.id.topOverlay),
            bottomOverlay = mainBinding.root.findViewById(R.id.bottomOverlay),
            zoomLayout = mainBinding.root.findViewById(R.id.zoomLayout)
        )
    }
    private fun scanFace() {
        imageAnalyzer.setShouldAnalyze(true)
        buttonUtility.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
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