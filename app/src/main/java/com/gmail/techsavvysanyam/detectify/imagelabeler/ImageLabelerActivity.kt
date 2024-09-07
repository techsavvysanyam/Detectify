package com.gmail.techsavvysanyam.detectify.imagelabeler

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityImageLabelerBinding
import com.gmail.techsavvysanyam.detectify.util.CommonButtonUtility
import com.gmail.techsavvysanyam.detectify.util.ImageManager
import com.gmail.techsavvysanyam.detectify.util.PermissionUtility
import com.gmail.techsavvysanyam.detectify.util.ScreenshotUtility
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageLabelerActivity : AppCompatActivity() {
    private val mainBinding: ActivityImageLabelerBinding by lazy {
        ActivityImageLabelerBinding.inflate(layoutInflater)
    }
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
            analyzeLabelFromImage(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        if (PermissionUtility.checkMultiplePermission(this, multiplePermissionNameList.toTypedArray(), multiplePermissionId)) {
            initializeButtonUtility()
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

    private fun playClickAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.shutter_animation)
        mainBinding.previewImage.startAnimation(animation)
    }

    private fun playClickSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    private fun takePicture() {
        val imageCapture = buttonUtility.imageCapture ?: return
        // Play click sound and animation
        playClickSound()
        playClickAnimation()

        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Uri.fromFile(photoFile)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    analyzeLabelFromImage(bitmap)
                    ImageManager.saveImage(this@ImageLabelerActivity, photoFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@ImageLabelerActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
            previewView = mainBinding.previewImage,
            restartButton = null,
            flashButton = mainBinding.root.findViewById(R.id.flashToggleIB),
            flipCameraButton = mainBinding.root.findViewById(R.id.flipCameraIB),
            camera = null,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            toggleButton = mainBinding.root.findViewById(R.id.frameToggleButton),
            topOverlay = mainBinding.root.findViewById(R.id.topOverlay),
            bottomOverlay = mainBinding.root.findViewById(R.id.bottomOverlay),
            zoomLayout = mainBinding.root.findViewById(R.id.zoomLayout)
        )
        buttonUtility.startCamera()
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun analyzeLabelFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    ScreenshotUtility.showCustomToast(this, "No labels detected in captured image")
                    return@addOnSuccessListener
                }
                // Collect labels and their confidence levels
                val analyzedImage = labels.joinToString("\n") { "${it.text} - ${it.confidence}" }
                // Pass the labels to AnalyzedImageActivity
                val intent = Intent(this, AnalyzedImageActivity::class.java).apply {
                    putExtra("analyzedImage", analyzedImage)
                }
                startActivity(intent)
            }
            .addOnFailureListener { _ ->
                Toast.makeText(this, "Failed to label image", Toast.LENGTH_SHORT).show()
            }
    }
}
