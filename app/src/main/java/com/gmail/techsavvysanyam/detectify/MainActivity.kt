package com.gmail.techsavvysanyam.detectify

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.gmail.techsavvysanyam.detectify.barcode.BarcodeActivity
import com.gmail.techsavvysanyam.detectify.databinding.ActivityMainBinding
import com.gmail.techsavvysanyam.detectify.facedetect.FaceDetectActivity
import com.gmail.techsavvysanyam.detectify.imagelabeler.ImageLabelerActivity
import com.gmail.techsavvysanyam.detectify.objectdetection.ObjectDetectActivity
import com.gmail.techsavvysanyam.detectify.textrecognition.TextRgActivity

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)

        mainBinding.themeSwitcher.setOnClickListener {
            switchTheme()
        }
        mainBinding.openBarcode.setOnClickListener {
            startActivity(Intent(this, BarcodeActivity::class.java))
        }
        mainBinding.openFaceDetection.setOnClickListener {
            startActivity(Intent(this, FaceDetectActivity::class.java))
        }
        mainBinding.openTextRecognizer.setOnClickListener {
            startActivity(Intent(this, TextRgActivity::class.java))
        }
        mainBinding.openObjectDetector.setOnClickListener{
            startActivity(Intent(this, ObjectDetectActivity::class.java))
        }
        mainBinding.openImageLabeler.setOnClickListener{
            startActivity(Intent(this, ImageLabelerActivity::class.java))
        }
    }

    private fun switchTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }
}
