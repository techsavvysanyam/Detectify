@file:Suppress("DEPRECATION")

package com.gmail.techsavvysanyam.detectify.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.gmail.techsavvysanyam.detectify.R
import java.io.OutputStream

object ScreenshotUtility {

    fun captureScreenshot(context: Context, rootView: View, surfaceView: SurfaceView, resultsView: View, onFinish: () -> Unit) {
        val cameraBitmap = Bitmap.createBitmap(surfaceView.height, surfaceView.width, Bitmap.Config.ARGB_8888)

        // Start fade-out animation
        rootView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out))

        val startTime = System.currentTimeMillis()
        PixelCopy.request(surfaceView, cameraBitmap, { copyResult ->
            val pixelCopyTime = System.currentTimeMillis()
            if (copyResult == PixelCopy.SUCCESS) {
                Log.d("Screenshot", "PixelCopy capture time: ${pixelCopyTime - startTime} ms")

                // Capture the results bitmap
                val resultsBitmap = captureResultsBitmap(resultsView)

                // Overlay the results bitmap on the camera bitmap
                val combinedBitmap = overlayResultsOnCameraBitmap(cameraBitmap, resultsBitmap)

                // Save the screenshot in a background thread
                Thread {
                    saveScreenshot(context, combinedBitmap) { uri ->
                        // Start fade-in animation after saving screenshot
                        Handler(Looper.getMainLooper()).post {
                            rootView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                            displayOverlay(context, combinedBitmap, uri)
                            onFinish()
                            //Custom Toast
                            showCustomToast(context, "Screenshot saved to gallery")
                        }
                    }
                }.start()
            } else {
                Handler(Looper.getMainLooper()).post {
                    // Start fade-in animation if PixelCopy failed
                    rootView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                    showCustomToast(context, "Failed to capture screenshot")
                }
            }
        }, Handler(Looper.getMainLooper()))
    }


    private fun captureResultsBitmap(view: View): Bitmap {
        val resultsBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultsBitmap)
        view.draw(canvas)
        return resultsBitmap
    }

    private fun overlayResultsOnCameraBitmap(cameraBitmap: Bitmap, resultsBitmap: Bitmap): Bitmap {
        val combinedBitmap = Bitmap.createBitmap(cameraBitmap.width, cameraBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)

        // Draw the camera bitmap
        canvas.drawBitmap(cameraBitmap, 0f, 0f, null)

        // Draw the results bitmap at the bottom of the camera bitmap
        val left = 0f
        val top = cameraBitmap.height - resultsBitmap.height.toFloat()
        canvas.drawBitmap(resultsBitmap, left, top, null)

        return combinedBitmap
    }

    private fun saveScreenshot(context: Context, bitmap: Bitmap, onSuccess: (Uri) -> Unit) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(uri)
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
                    onSuccess(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
                }
            } finally {
                outputStream?.close()
            }
        }
    }


    private fun displayOverlay(context: Context, bitmap: Bitmap, uri: Uri) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.screenshot_overlay)

        val btnShare = dialog.findViewById<ImageButton>(R.id.btn_share)
        val btnDelete = dialog.findViewById<ImageButton>(R.id.btn_delete)
        val overlayLayout = dialog.findViewById<LinearLayout>(R.id.overlay_layout)

        btnShare.setOnClickListener {
            shareScreenshot(context, bitmap)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            deleteScreenshot(context, uri)
            dialog.dismiss()
        }

        dialog.show()

        // Add slide-in animation for the overlay
        val slideIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
        overlayLayout.startAnimation(slideIn)

        // Automatically hide the overlay after a delay (e.g., 5 seconds)
        val autoHideDelay = 5000L // 5000 milliseconds = 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                // Add slide-out animation before hiding the overlay
                val slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                overlayLayout.startAnimation(slideOut)

                // Dismiss the dialog after the slide-out animation completes
                slideOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        dialog.dismiss()
                    }
                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }, autoHideDelay)
    }

    private fun shareScreenshot(context: Context, bitmap: Bitmap) {
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Screenshot", null)
        val uri = Uri.parse(path)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        context.startActivity(Intent.createChooser(intent, "Share Screenshot"))
    }

    private fun deleteScreenshot(context: Context, uri: Uri) {
        val resolver = context.contentResolver
        try {
            resolver.delete(uri, null, null)
            Handler(Looper.getMainLooper()).post {
                showCustomToast(context, "Screenshot deleted")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                showCustomToast(context, "Failed to delete screenshot")
            }
        }
    }
    @SuppressLint("InflateParams")
    internal fun showCustomToast(context: Context, message: String) {
        // Inflate the custom Toast layout
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        // Find the TextView in the custom layout and set the text
        val toastText = layout.findViewById<TextView>(R.id.toast_text)
        toastText.text = message

        // Create and show the custom Toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout

        // Convert 200dp to pixels
        val marginBottomInDp = 280
        val marginBottomInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginBottomInDp.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()

        // Set the custom position for the Toast
        val gravity = Gravity.BOTTOM or Gravity.END
        val xOffset = 0

        toast.setGravity(gravity, xOffset, marginBottomInPx)
        // Show the toast
        toast.show()

    }
}


