package com.gmail.techsavvysanyam.detectify.textrecognition

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityTextRgBinding
import com.gmail.techsavvysanyam.detectify.util.CommonButtonUtility
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TextRgActivity : AppCompatActivity(), TextRgAnalyzer.TextRgResultCallback {
    private val mainBinding: ActivityTextRgBinding by lazy {
        ActivityTextRgBinding.inflate(layoutInflater)
    }
    private val imageAnalyzer = TextRgAnalyzer(this)
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

    private val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            recognizeTextFromImage(bitmap)
        }
    }

    private val takePictureLauncher: ActivityResultLauncher<Uri> = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                recognizeTextFromImage(bitmap)
            }
        }
    }

    private var currentPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        setStatusBarColor(R.color.status_bar_blue)
        if (PermissionUtility.checkMultiplePermission(this, multiplePermissionNameList.toTypedArray(), multiplePermissionId)) {
            initializeButtonUtility()
        }

        mainBinding.TextScanIB.setOnClickListener {
            mainBinding.imageView.visibility = View.GONE
            scanText()
        }
        mainBinding.pickImageButton.setOnClickListener {
            pickImageFromGallery()
        }
        mainBinding.takePictureButton.setOnClickListener {
            takePicture()
        }
        mainBinding.root.findViewById<View>(R.id.screenshotButton).visibility = View.GONE
        mainBinding.root.findViewById<View>(R.id.restartButton).visibility = View.GONE
    }
    private fun takePicture() {
        val imageFile = createImageFile()
        val photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )
        currentPhotoUri = photoUri
        ImageManager.saveImage(this, imageFile.absolutePath)
        takePictureLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm-ss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timestamp}_",
            ".jpg",
            storageDir
        )
    }

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
            previewView = mainBinding.previewText,
            restartButton = null,
            flashButton = mainBinding.root.findViewById(R.id.flashToggleIB),
            flipCameraButton = mainBinding.root.findViewById(R.id.flipCameraIB),
            camera = null,
            lensFacing = CameraSelector.LENS_FACING_FRONT
        )
    }

    private fun scanText() {
        imageAnalyzer.setShouldAnalyze(true)
        buttonUtility.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.textBlocks.joinToString("\n") { it.text }
                // Pass recognized text to a new activity
                val intent = Intent(this, RecognizedTextActivity::class.java)
                intent.putExtra("recognizedText", recognizedText)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to recognize text from image", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onTextRgLine(text: String?) {
        mainBinding.textRgLine.text = text
    }
}
