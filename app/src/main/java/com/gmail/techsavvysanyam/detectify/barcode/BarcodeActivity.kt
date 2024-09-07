package com.gmail.techsavvysanyam.detectify.barcode

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
import com.gmail.techsavvysanyam.detectify.databinding.ActivityBarcodeBinding
import com.gmail.techsavvysanyam.detectify.util.CommonButtonUtility
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.gmail.techsavvysanyam.detectify.util.ScreenshotUtility

class BarcodeActivity : AppCompatActivity(), BarcodeAnalyzer.BarcodeResultCallback {
    private val mainBinding: ActivityBarcodeBinding by lazy {
        ActivityBarcodeBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = BarcodeAnalyzer(this, this)
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

        mainBinding.BarcodeScanIB.setOnClickListener {
            scanBarcode()
        }
//        mainBinding.toggleButton.setOnClickListener {
//            areOverlaysVisible = !areOverlaysVisible
//            toggleOverlays()
//        }
        mainBinding.root.findViewById<View>(R.id.screenshotButton).setOnClickListener {
            ScreenshotUtility.captureScreenshot(
                this,
                mainBinding.root,
                mainBinding.previewBarcode.getChildAt(0) as SurfaceView,
                mainBinding.barcodeResultTextView
            ) {}
        }

        val copyBarcode = mainBinding.copyBarcode
        copyBarcode.setOnClickListener {
            val barcodeText = mainBinding.barcodeResultTextView.text
            if (!barcodeText.isNullOrEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Barcode", barcodeText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Barcode copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No barcode to copy", Toast.LENGTH_SHORT).show()
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
            previewView = mainBinding.previewBarcode,
            restartButton = mainBinding.root.findViewById(R.id.restartButton),
            flashButton = mainBinding.root.findViewById(R.id.flashToggleIB),
            flipCameraButton = mainBinding.root.findViewById(R.id.flipCameraIB),
            camera = null,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            toggleButton = mainBinding.root.findViewById(R.id.frameToggleButton),
            topOverlay = mainBinding.root.findViewById(R.id.topOverlay),
            bottomOverlay = mainBinding.root.findViewById(R.id.bottomOverlay),
            zoomLayout = mainBinding.root.findViewById(R.id.zoomLayout)
        )
    }

    private fun scanBarcode() {
        imageAnalyzer.setShouldAnalyze(true)
        buttonUtility.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
    }

    override fun onBarcodeDetected(barcode: String?) {
        runOnUiThread {
            mainBinding.barcodeResultTextView.text = barcode
        }
    }
}

