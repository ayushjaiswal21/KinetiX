package com.asdevs.kinematix

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setupGenderDropdown()
        setupProfileSubmission()
    }

    private fun setupGenderDropdown() {
        val genderDropdown = findViewById<AutoCompleteTextView>(R.id.genderDropdown)
        val genders = arrayOf("Male", "Female", "Other")

        val adapter = ArrayAdapter(
            this,
            R.layout.item_gender_dropdown,
            genders
        )

        genderDropdown.setAdapter(adapter)
        genderDropdown.setOnItemClickListener { parent, _, position, _ ->
            selectedGender = parent.getItemAtPosition(position) as String
        }
    }

    private fun setupProfileSubmission() {
        findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.etName).text.toString()
            val age = findViewById<TextInputEditText>(R.id.etAge).text.toString()
            val weight = findViewById<TextInputEditText>(R.id.etWeight).text.toString()
            val height = findViewById<TextInputEditText>(R.id.etHeight).text.toString()

            when {
                name.isEmpty() -> showError("Please enter your name")
                age.isEmpty() -> showError("Please enter your age")
                weight.isEmpty() -> showError("Please enter your weight")
                height.isEmpty() -> showError("Please enter your height")
                selectedGender == null -> showError("Please select your gender")
                else -> saveProfile(name, age, weight, height)
            }
        }
    }

    private fun saveProfile(name: String, age: String, weight: String, height: String) {
        val user = auth.currentUser ?: return
        val userData = hashMapOf(
            "name" to name,
            "age" to age.toInt(),
            "weight" to weight.toFloat(),
            "height" to height.toFloat(),
            "gender" to selectedGender,
            "email" to user.email,
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )

        findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = false
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity() // Clear activity stack
            }
            .addOnFailureListener { e ->
                findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = true
                showError("Failed to save profile: ${e.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}