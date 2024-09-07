package com.gmail.techsavvysanyam.detectify.util

import android.animation.ObjectAnimator
import android.app.Activity
import android.view.Surface
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.gmail.techsavvysanyam.detectify.R
import kotlin.math.abs

class CommonButtonUtility(
    private val activity: Activity,
    private val previewView: PreviewView?,
    private val restartButton: ImageButton?,
    private val flashButton: ImageButton,
    private var camera: Camera?,
    private var lensFacing: Int,
    private var flipCameraButton: ImageButton,
    private var toggleButton: ImageButton,
    private var topOverlay: View,
    private var bottomOverlay: View,
    private val zoomLayout: LinearLayout?

) {
    var imageCapture: ImageCapture? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    internal lateinit var imageAnalysis: ImageAnalysis
    private var areOverlaysVisible: Boolean = true // Initial state is true

    init {
        topOverlay.visibility = View.VISIBLE
        bottomOverlay.visibility = View.VISIBLE
        topOverlay.alpha = 1f
        bottomOverlay.alpha = 1f

        startCamera()
        setupRestartButton()
        setupFlashButton()
        setFlipCameraButton()
        setToggleButton()
        initializeZoomButtons()

    }

    internal fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun bindCameraUserCases() {
        val screenAspectRatio = previewView?.let { aspectRatio(it.width, previewView.height) }
        val rotation = previewView?.display?.rotation ?: Surface.ROTATION_0
        val resolutionSelector =
            screenAspectRatio?.let {
                AspectRatioStrategy(
                    it,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            }
                ?.let {
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(it)
                        .build()
                }

        val preview = resolutionSelector?.let {
            Preview.Builder()
                .setResolutionSelector(it)
                .setTargetRotation(rotation)
                .build()
                .also { preview ->
                    preview.setSurfaceProvider(previewView?.surfaceProvider)
                }
        }

        imageAnalysis = ImageAnalysis.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                activity as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            // Ensure imageCapture is set up properly
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.bindToLifecycle(activity as LifecycleOwner, cameraSelector, imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun setupRestartButton() {
        restartButton?.setOnClickListener {
            restartActivityWithAnimation()
        }
    }

    private fun setupFlashButton() {
        flashButton.setOnClickListener {
            camera?.let { setFlashIcon(it) }
        }
    }

    private fun setFlashIcon(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                flashButton.setImageResource(R.drawable.flash_on)
            } else {
                camera.cameraControl.enableTorch(false)
                flashButton.setImageResource(R.drawable.flash_off)
            }
        } else {
            Toast.makeText(activity, "Flash not supported", Toast.LENGTH_SHORT).show()
            flashButton.isEnabled = false
        }
    }

    private fun restartActivityWithAnimation() {
        val intent = activity.intent
        val options = ActivityOptionsCompat.makeCustomAnimation(
            activity,
            R.anim.fade_in, // Enter animation
            R.anim.fade_out // Exit animation
        )
        activity.finish()
        ContextCompat.startActivity(activity, intent, options.toBundle())
    }

    private fun setFlipCameraButton() {
        flipCameraButton.setOnClickListener {
            val rotationAnimator = ObjectAnimator.ofFloat(flipCameraButton, "rotation", 0f, 360f)
            rotationAnimator.duration = 800
            rotationAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotationAnimator.start()
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCases()
        }
    }

    private fun setToggleButton() {
        toggleButton.setOnClickListener {
            toggleOverlays()
        }
    }

    private fun toggleOverlays() {
        val duration = 300L

        if (areOverlaysVisible) {
            topOverlay.animate().alpha(0f).setDuration(duration).withEndAction {
                topOverlay.visibility = View.GONE
            }.start()
            bottomOverlay.animate().alpha(0f).setDuration(duration).withEndAction {
                bottomOverlay.visibility = View.GONE
            }.start()
            toggleButton.setImageResource(R.drawable.ic_16_9)
        } else {
            topOverlay.visibility = View.VISIBLE
            bottomOverlay.visibility = View.VISIBLE
            topOverlay.alpha = 0f
            bottomOverlay.alpha = 0f
            topOverlay.animate().alpha(1f).setDuration(duration).start()
            bottomOverlay.animate().alpha(1f).setDuration(duration).start()
            toggleButton.setImageResource(R.drawable.ic_4_3)
        }

        areOverlaysVisible = !areOverlaysVisible // Update the flag
    }

    private fun initializeZoomButtons() {
        zoomLayout?.findViewById<TextView>(R.id.zoom_1x)?.setOnClickListener {
            setZoomLevel(1f)
            updateZoomButtons(R.id.zoom_1x)
        }

        zoomLayout?.findViewById<TextView>(R.id.zoom_2x)?.setOnClickListener {
            setZoomLevel(2f)
            updateZoomButtons(R.id.zoom_2x)
        }
    }

    private fun setZoomLevel(zoom: Float) {
        camera?.cameraControl?.setZoomRatio(zoom) // Use setZoomRatio for actual zoom levels
    }

    private fun updateZoomButtons(selectedButtonId: Int) {
        zoomLayout?.findViewById<TextView>(R.id.zoom_1x)?.apply {
            setBackgroundResource(
                if (selectedButtonId == R.id.zoom_1x) R.drawable.zoom_toggle_selected_background
                else android.R.color.transparent
            )
            setTextColor(
                if (selectedButtonId == R.id.zoom_1x) context.getColor(R.color.magicBlue)
                else context.getColor(R.color.colorTextPrimary)
            )
            text = if (selectedButtonId == R.id.zoom_1x) "1x" else "1"
            textSize = if (selectedButtonId == R.id.zoom_1x) 14f else 12f // Adjust text size as needed
        }

        zoomLayout?.findViewById<TextView>(R.id.zoom_2x)?.apply {
            setBackgroundResource(
                if (selectedButtonId == R.id.zoom_2x) R.drawable.zoom_toggle_selected_background
                else android.R.color.transparent
            )
            setTextColor(
                if (selectedButtonId == R.id.zoom_2x) context.getColor(R.color.magicBlue)
                else context.getColor(R.color.colorTextPrimary)
            )
            text = if (selectedButtonId == R.id.zoom_2x) "2x" else "2"
            textSize = if (selectedButtonId == R.id.zoom_2x) 14f else 12f // Adjust text size as needed
        }
    }
}








