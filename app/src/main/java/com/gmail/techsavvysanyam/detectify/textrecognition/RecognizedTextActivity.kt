package com.gmail.techsavvysanyam.detectify.textrecognition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityRecognizedTextBinding

class RecognizedTextActivity : AppCompatActivity() {
    private val recognizedTextBinding: ActivityRecognizedTextBinding by lazy {
        ActivityRecognizedTextBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(recognizedTextBinding.root)
        setStatusBarColor(R.color.status_bar_blue)

        val recognizedText = intent.getStringExtra("recognizedText")
        recognizedTextBinding.recognizedTextView.text = recognizedText

        recognizedTextBinding.themeSwitcher.setOnClickListener {
            switchTheme()
        }

        recognizedTextBinding.copyText.setOnClickListener {
            val copyText = recognizedTextBinding.recognizedTextView.text
            if (!copyText.isNullOrEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Text copied", copyText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Whole text copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No TEXT to copy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // status bar color
    private fun setStatusBarColor(colorResId: Int) {
        window.statusBarColor = resources.getColor(colorResId, theme)
    }

    // theme switcher
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
