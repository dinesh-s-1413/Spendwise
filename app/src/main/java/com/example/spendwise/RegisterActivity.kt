package com.example.spendwise

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.spendwise.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupCountryDropdown()

        // Register button click
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val country = binding.actvCountry.text.toString().trim()

            // Validation
            if (name.isEmpty()) {
                binding.nameLayout.error = "Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.emailLayout.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.passwordLayout.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.passwordLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.confirmPasswordLayout.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (country.isEmpty()) {
                binding.countryLayout.error = "Please select your country"
                return@setOnClickListener
            }

            // Clear errors
            binding.nameLayout.error = null
            binding.emailLayout.error = null
            binding.passwordLayout.error = null
            binding.confirmPasswordLayout.error = null
            binding.countryLayout.error = null

            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Creating account..."

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid

                        // Save country to SharedPreferences
                        CurrencyHelper.saveCountry(this, country)

                        // Get currency symbol
                        val currencySymbol = CurrencyHelper.getCurrencySymbol(this)

                        // Save user info in Realtime Database
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference("users").child(userId)

                        val userMap = mapOf(
                            "name" to name,
                            "email" to email,
                            "country" to country,
                            "currency" to currencySymbol,
                            "createdAt" to System.currentTimeMillis()
                        )

                        userRef.setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Account created successfully! 🎉",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                                binding.btnRegister.isEnabled = true
                                binding.btnRegister.text = "Create Account"
                            }
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Create Account"
                    }
                }
        }

        // Go back to Login
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupCountryDropdown() {
        val countries = CurrencyHelper.getCountryNames()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            countries
        )
        binding.actvCountry.setAdapter(adapter)

        // Auto select India as default
        binding.actvCountry.setText("India", false)
        CurrencyHelper.saveCountry(this, "India")
    }
}