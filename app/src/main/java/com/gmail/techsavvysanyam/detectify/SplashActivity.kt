package com.gmail.techsavvysanyam.detectify

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.gmail.techsavvysanyam.detectify.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDisplayLength = 1200L
    private val mainBinding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(mainBinding.root)

        val progressBar = mainBinding.progressBar
        val loadBarUpLogo = mainBinding.loadBarUpLogo

        val mainIntent = Intent(this@SplashActivity, MainActivity::class.java)

        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = splashDisplayLength
        animator.addUpdateListener { animation ->
            val progressStatus = animation.animatedValue as Int
            progressBar.progress = progressStatus
            val arrowPosition = progressBar.width * progressStatus / progressBar.max
            loadBarUpLogo.translationX = arrowPosition.toFloat()
        }
        animator.start()
        animator.doOnEnd {
            startActivity(mainIntent)
            finish()
        }
    }
}
