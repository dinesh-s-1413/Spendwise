package com.example.spendwise

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.spendwise.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Bounce logo first
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        binding.ivLogo.startAnimation(bounce)

        // Start typewriter effect after logo bounces
        handler.postDelayed({
            startTypewriterEffect("SpendWise") {
                // After name is typed show tagline
                startTaglineTypewriter("Smart Subscription Manager") {
                    // Navigate after everything is typed
                    handler.postDelayed({
                        navigateToNext()
                    }, 800)
                }
            }
        }, 1000)
    }

    private fun startTypewriterEffect(text: String, onComplete: () -> Unit) {
        var index = 0
        val typewriterRunnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    binding.tvAppName.text = text.substring(0, index)
                    index++
                    handler.postDelayed(this, 100) // type speed
                } else {
                    onComplete()
                }
            }
        }
        handler.post(typewriterRunnable)
    }

    private fun startTaglineTypewriter(text: String, onComplete: () -> Unit) {
        var index = 0
        binding.tvTagline.text = ""
        val typewriterRunnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    binding.tvTagline.text = text.substring(0, index)
                    index++
                    handler.postDelayed(this, 50) // faster for tagline
                } else {
                    onComplete()
                }
            }
        }
        handler.post(typewriterRunnable)
    }

    private fun navigateToNext() {
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}