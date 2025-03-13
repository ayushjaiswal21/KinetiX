package com.asdevs.kinematix

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupSignUp()
        setupSignInLink()
    }

    private fun setupSignUp() {
        findViewById<MaterialButton>(R.id.btnSignUp).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.etName).text.toString()
            val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString()
            val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()
            val confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword).text.toString()

            // Validation
            when {
                name.isEmpty() -> showError("Please enter your name")
                email.isEmpty() -> showError("Please enter your email")
                !isValidEmail(email) -> showError("Please enter a valid email")
                password.isEmpty() -> showError("Please enter a password")
                password.length < 6 -> showError("Password must be at least 6 characters")
                password != confirmPassword -> showError("Passwords do not match")
                else -> createAccount(name, email, password)
            }
        }
    }

    private fun createAccount(name: String, email: String, password: String) {
        findViewById<MaterialButton>(R.id.btnSignUp).isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                startActivity(Intent(this, ProfileSetupActivity::class.java))
                                finish()
                            } else {
                                showError("Failed to update profile")
                            }
                        }
                } else {
                    showError(task.exception?.message ?: "Registration failed")
                }
                findViewById<MaterialButton>(R.id.btnSignUp).isEnabled = true
            }
    }

    private fun setupSignInLink() {
        findViewById<TextView>(R.id.tvSignIn).setOnClickListener {
            finish()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}