package com.gmail.techsavvysanyam.detectify.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.animation.AnimationUtils
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
                    saveScreenshot(context, combinedBitmap) {
                        // Start fade-in animation after saving screenshot
                        Handler(Looper.getMainLooper()).post {
                            rootView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                            onFinish()
                        }
                    }
                }.start()
            } else {
                Handler(Looper.getMainLooper()).post {
                    // Start fade-in animation if PixelCopy failed
                    rootView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                    Toast.makeText(context, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
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

    private fun saveScreenshot(context: Context, bitmap: Bitmap, onSuccess: () -> Unit) {
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
                    onSuccess()
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
}
