package com.gmail.techsavvysanyam.detectify.imagelabeler

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.gmail.techsavvysanyam.detectify.R
import com.gmail.techsavvysanyam.detectify.databinding.ActivityAnalyzedImageBinding

class AnalyzedImageActivity : AppCompatActivity() {
    private val analyzedImageBinding: ActivityAnalyzedImageBinding by lazy {
        ActivityAnalyzedImageBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(analyzedImageBinding.root)
        val analyzedImage = intent.getStringExtra("analyzedImage")
        populateTable(analyzedImage)
        analyzedImageBinding.themeSwitcher.setOnClickListener {
            switchTheme()
        }
        analyzedImageBinding.copyText.setOnClickListener {
            copyTableDataToClipboard(analyzedImage)
        }
    }
    private fun copyTableDataToClipboard(analyzedImage: String?) {
        if (!analyzedImage.isNullOrEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Text copied", analyzedImage)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.no_text_to_copy, Toast.LENGTH_SHORT).show()
        }
    }
    // Populate TableLayout with analyzed image data
    private fun populateTable(analyzedImage: String?) {
        val tableLayout = analyzedImageBinding.labelsTableLayout
        tableLayout.removeAllViews()

        // Add table header
        val headerRow = TableRow(this)
        val labelHeader = TextView(this).apply {
            text = getString(R.string.label_header)
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }
        val confidenceHeader = TextView(this).apply {
            text = getString(R.string.confidence_header)
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }
        headerRow.addView(labelHeader)
        headerRow.addView(confidenceHeader)
        tableLayout.addView(headerRow)

        // Parse and add rows
        analyzedImage?.let {
            val lines = it.split("\n")
            lines.forEach { line ->
                val parts = line.split(" - ")
                if (parts.size == 2) {
                    val row = TableRow(this)
                    val labelTextView = TextView(this).apply {
                        text = parts[0]
                        setPadding(8, 8, 8, 8)
                    }
                    val confidenceTextView = TextView(this).apply {
                        text = parts[1]
                        setPadding(8, 8, 8, 8)
                    }
                    row.addView(labelTextView)
                    row.addView(confidenceTextView)
                    tableLayout.addView(row)
                }
            }
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
