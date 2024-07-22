package com.gmail.techsavvysanyam.detectify.util

import android.app.Activity
import android.view.Surface
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
    private val previewView: PreviewView,
    private val restartButton: ImageButton?,
    private val flashButton: ImageButton,
    private var camera: Camera?,
    private var flipCameraButton: ImageButton,
    private var lensFacing: Int
) {

    private lateinit var cameraProvider: ProcessCameraProvider
    internal lateinit var imageAnalysis: ImageAnalysis

    init {
        setupRestartButton()
        setupFlashButton()
        setFlipCameraButton()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun bindCameraUserCases() {
        val screenAspectRatio = aspectRatio(previewView.width, previewView.height)
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(screenAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageAnalysis = ImageAnalysis.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(activity as LifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
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
                flashButton.setImageResource(R.drawable.flash_off)
            } else {
                camera.cameraControl.enableTorch(false)
                flashButton.setImageResource(R.drawable.flash_on)
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
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCases()
        }
        }
    }



