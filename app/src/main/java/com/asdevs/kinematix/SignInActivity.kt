package com.asdevs.kinematix

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private lateinit var loadingDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupGoogleSignIn()
        setupEmailSignIn()
        setupSignUpLink()
        setupForgotPassword()
    }

    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            findViewById<MaterialButton>(R.id.btnGoogleSignIn).setOnClickListener {
                showLoading()
                signInWithGoogle()
            }
        } catch (e: Exception) {
            Log.e("SignIn", "Error setting up Google Sign In: ${e.message}")
            showError("Failed to initialize Google Sign In")
        }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            hideLoading()
            Log.e("SignIn", "Error launching Google Sign In: ${e.message}")
            showError("Failed to start Google Sign In")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        Log.d("SignIn", "Google Sign In successful")
                        firebaseAuthWithGoogle(account.idToken!!)
                    } else {
                        hideLoading()
                        showError("Google sign in failed: Account is null")
                    }
                } catch (e: ApiException) {
                    hideLoading()
                    Log.e("SignIn", "Google Sign In failed: ${e.statusCode}")
                    showError("Google sign in failed: ${getGoogleSignInErrorMessage(e.statusCode)}")
                }
            } catch (e: Exception) {
                hideLoading()
                Log.e("SignIn", "Error in onActivityResult: ${e.message}")
                showError("Google sign in failed")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SignIn", "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user profile exists
                        db.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Create new user profile
                                    createUserProfile(user)
                                } else {
                                    // Profile exists, go to main activity
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                            }
                            .addOnFailureListener { e ->
                                hideLoading()
                                Log.e("SignIn", "Error checking user profile", e)
                                showError("Failed to check user profile")
                            }
                    }
                } else {
                    hideLoading()
                    Log.e("SignIn", "signInWithCredential:failure", task.exception)
                    showError("Authentication Failed: ${task.exception?.message}")
                }
            }
    }

    private fun createUserProfile(user: FirebaseUser) {
        val userProfile = hashMapOf(
            "email" to user.email,
            "name" to user.displayName,
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(userProfile)
            .addOnSuccessListener {
                hideLoading()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("SignIn", "Error creating user profile", e)
                showError("Failed to create user profile")
            }
    }


    private fun setupEmailSignIn() {
        findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString()
            val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()


            when {
                email.isEmpty() -> showError("Please enter your email")
                !isValidEmail(email) -> showError("Please enter a valid email")
                password.isEmpty() -> showError("Please enter your password")
                else -> signInWithEmail(email, password)
            }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        findViewById<MaterialButton>(R.id.btnSignIn).isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserProfileAndNavigate()
                } else {
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException ->
                            showError("Invalid email or password")
                        is FirebaseAuthInvalidUserException ->
                            showError("No account found with this email")
                        else -> showError("Authentication failed: ${task.exception?.message}")
                    }
                }
                findViewById<MaterialButton>(R.id.btnSignIn).isEnabled = true
            }
    }

    private fun checkUserProfileAndNavigate() {
        showLoading()
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                hideLoading()
                if (document.exists()) {
                    // User profile exists, go to main activity
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Create new user profile with Google account info
                    createUserProfile(user)
                }
                finish()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("SignIn", "Error checking user profile", e)
                startActivity(Intent(this, ProfileSetupActivity::class.java))
                finish()
            }
    }

    private fun getGoogleSignInErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
            GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred"
            GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
            GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
            else -> "Unknown error occurred"
        }
    }


    private fun setupSignUpLink() {
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun setupForgotPassword() {
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString()
            when {
                email.isEmpty() -> showError("Please enter your email")
                !isValidEmail(email) -> showError("Please enter a valid email")
                else -> sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSuccess("Password reset email sent")
                } else {
                    showError("Failed to send reset email: ${task.exception?.message}")
                }
            }
    }

    private fun showLoading() {
        loadingDialog.show()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}