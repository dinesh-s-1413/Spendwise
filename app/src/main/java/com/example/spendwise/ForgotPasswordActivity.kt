package com.example.spendwise

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.spendwise.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // ── Back button (top left arrow) ───────────────────────────────────────
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // ── Send Reset Link button ─────────────────────────────────────────────
        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // Validate
            if (email.isEmpty()) {
                binding.emailLayout.error = "Please enter your email"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Enter a valid email address"
                return@setOnClickListener
            }

            binding.emailLayout.error = null
            setLoadingState(true)

            // Send reset email via Firebase
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    setLoadingState(false)
                    if (task.isSuccessful) {
                        showSuccessState(email)
                    } else {
                        val msg = when {
                            task.exception?.message?.contains("no user record") == true ->
                                "No account found with this email address."
                            task.exception?.message?.contains("network") == true ->
                                "Network error. Please check your connection."
                            else -> task.exception?.message ?: "Failed to send reset email."
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
        }

        // ── "Sign In" link at bottom ───────────────────────────────────────────
        binding.tvBackToLogin.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Clear error on typing
        binding.etEmail.setOnFocusChangeListener { _, _ ->
            binding.emailLayout.error = null
        }
    }

    // ── Show success state after email is sent ─────────────────────────────────
    private fun showSuccessState(email: String) {
        // Hide the card form
        binding.root.findViewWithTag<View?>("cardForm")

        // Hide send button, show success message
        binding.btnSendReset.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.tvSuccessEmail.text = "We sent a reset link to\n$email\n\nCheck your inbox (and spam folder)."

        // Disable the email field
        binding.etEmail.isEnabled = false
    }

    // ── Loading state while Firebase call is in progress ──────────────────────
    private fun setLoadingState(loading: Boolean) {
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text = if (loading) "Sending…" else "Send Reset Link"
        binding.etEmail.isEnabled = !loading
        val email = ""
        binding.tvSuccessEmail.text = "We sent a reset link to\n$email\n\n⚠️ Check your spam/junk folder if you don't see it in inbox."
    }
}