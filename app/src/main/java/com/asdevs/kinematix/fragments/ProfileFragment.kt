package com.asdevs.kinematix.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.SignInActivity
import com.asdevs.kinematix.UserProfileManager
import com.asdevs.kinematix.database.FirestoreWorkoutRepository
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val workoutRepository = FirestoreWorkoutRepository()

    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var editProfileButton: ImageButton
    private lateinit var workoutCountText: TextView
    private lateinit var streakCountText: TextView
    private lateinit var achievementsRecyclerView: RecyclerView
    private lateinit var notificationSettingsButton: TextView
    private lateinit var subscriptionButton: TextView
    private lateinit var privacyButton: TextView
    private lateinit var logoutButton: MaterialButton
    private val userProfileManager = UserProfileManager.getInstance()
    private var loadingOverlay: View? = null
    private val PICK_IMAGE_REQUEST = 1
    private val PROFILE_IMAGE_NAME = "profile_image.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Initialize Google Sign In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        initializeViews(view)
        setupUserData()
        setupClickListeners()
        return view
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.iv_profile)
        usernameText = view.findViewById(R.id.tv_username)
        emailText = view.findViewById(R.id.tv_email)
        editProfileButton = view.findViewById(R.id.btnEditProfile)
        workoutCountText = view.findViewById(R.id.tv_workout_count)
        streakCountText = view.findViewById(R.id.tv_streak_count)
        notificationSettingsButton = view.findViewById(R.id.btn_notification_settings)
        privacyButton = view.findViewById(R.id.btn_privacy)
        logoutButton = view.findViewById(R.id.btn_logout)
    }

    private fun setupUserData() {
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            // Set basic user info
            usernameText.text = user.displayName ?: "User"
            emailText.text = user.email

            // Load profile image
            loadProfileImage()

            // Show loading state
            showLoading(true)

            // Load user stats using the repository
            workoutRepository.getUserStats(
                userId = user.uid,
                onSuccess = { totalWorkouts, currentStreak, bestStreak ->
                    if (isAdded) {
                        try {
                            // Update workout count
                            workoutCountText.text = totalWorkouts.toString()

                            // Update streak counts
                            streakCountText.text = currentStreak.toString()

                            showLoading(false)
                        } catch (e: Exception) {
                            Log.e("ProfileFragment", "Error updating UI: ${e.message}")
                            showError("Failed to update profile information")
                        }
                    }
                },
                onError = { e ->
                    if (isAdded) {
                        Log.e("ProfileFragment", "Error loading user stats: ${e.message}")
                        showError("Failed to load profile information")
                        showLoading(false)
                    }
                }
            )
        } ?: run {
            // Handle case where user is not logged in
            showError("User not logged in")
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        // Initialize views
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etAge = dialogView.findViewById<TextInputEditText>(R.id.etAge)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.etWeight)
        val etHeight = dialogView.findViewById<TextInputEditText>(R.id.etHeight)
        val rgGender = dialogView.findViewById<RadioGroup>(R.id.rgGender)

        showLoading(true)
        userProfileManager.getUserProfile(
            onSuccess = { profile ->
                showLoading(false)
                etName.setText(profile.name)
                etAge.setText(profile.age.toString())
                etWeight.setText(profile.weight.toString())
                etHeight.setText(profile.height.toString())

                // Set gender
                when (profile.gender) {
                    "Male" -> rgGender.check(R.id.rbMale)
                    "Female" -> rgGender.check(R.id.rbFemale)
                    else -> rgGender.check(R.id.rbOther)
                }
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to load profile: ${e.message}")
            }
        )

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = etName.text.toString()
                val age = etAge.text.toString()
                val weight = etWeight.text.toString()
                val height = etHeight.text.toString()
                val genderId = rgGender.checkedRadioButtonId

                when {
                    name.isEmpty() -> showError("Please enter your name")
                    age.isEmpty() -> showError("Please enter your age")
                    weight.isEmpty() -> showError("Please enter your weight")
                    height.isEmpty() -> showError("Please enter your height")
                    genderId == -1 -> showError("Please select your gender")
                    else -> {
                        val gender = when (genderId) {
                            R.id.rbMale -> "Male"
                            R.id.rbFemale -> "Female"
                            else -> "Other"
                        }
                        updateProfile(name, age, weight, height, gender)
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProfile(
        name: String,
        age: String,
        weight: String,
        height: String,
        gender: String
    ) {
        showLoading(true)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showError("User not logged in")
            return
        }

        val updates = hashMapOf(
            "name" to name,
            "age" to age.toInt(),
            "weight" to weight.toFloat(),
            "height" to height.toFloat(),
            "gender" to gender
        )

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                showLoading(false)
                showSuccess("Profile updated successfully")
                refreshProfileData() // Refresh the displayed profile data
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Failed to update profile: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay?.visibility = if (show) View.VISIBLE else View.GONE

        view?.apply {
            findViewById<ImageButton>(R.id.btnEditProfile)?.isEnabled = !show
            isEnabled = !show
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun refreshProfileData() {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        showLoading(true)
        userProfileManager.getUserProfile(
            onSuccess = { profile ->
                showLoading(false)
                usernameText.text = profile.name
                emailText.text = profile.email

                if (profile.photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profile.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile)
                }

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    FirebaseFirestore.getInstance()
                        .collection("workouts")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { documents ->
                            workoutCountText.text = documents.size().toString()

                            calculateWorkoutStreak(documents.documents) { streak ->
                                streakCountText.text = streak.toString()
                            }
                        }
                        .addOnFailureListener { e ->
                            showError("Failed to load workout stats: ${e.message}")
                            workoutCountText.text = "0"
                            streakCountText.text = "0"
                        }
                }
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to load profile: ${e.message}")

                val currentUser = auth.currentUser
                usernameText.text = currentUser?.displayName ?: "User"
                emailText.text = currentUser?.email
                profileImage.setImageResource(R.drawable.ic_profile)
                workoutCountText.text = "0"
                streakCountText.text = "0"
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data
            saveImageToInternalStorage(imageUri)
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri?) {
        if (imageUri == null) return

        val loadingDialog = AlertDialog.Builder(requireContext())
            .setView(layoutInflater.inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            // Get the bitmap from Uri
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, imageUri))
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            }

            // Save bitmap to internal storage
            val fileName = PROFILE_IMAGE_NAME
            requireContext().openFileOutput(fileName, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            // Get the saved file
            val savedFile = requireContext().getFileStreamPath(fileName)

            // Update UI in ProfileFragment
            Glide.with(this)
                .load(savedFile)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImage)

            // Notify HomeFragment to refresh
            val homeFragment = parentFragmentManager.fragments.find { it is HomeFragment } as? HomeFragment
            homeFragment?.loadProfileImage()

            loadingDialog.dismiss()
            showSuccess("Profile picture updated successfully")

        } catch (e: Exception) {
            loadingDialog.dismiss()
            showError("Failed to save image: ${e.message}")
        }
    }

    private fun loadProfileImage() {
        try {
            val fileName = PROFILE_IMAGE_NAME
            val file = requireContext().getFileStreamPath(fileName)

            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage)
            } else {
                Glide.with(this)
                    .load(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Failed to load profile image: ${e.message}")
        }
    }


    private fun calculateWorkoutStreak(workoutDocs: List<DocumentSnapshot>, onComplete: (Int) -> Unit) {
        var streak = 0
        val today = LocalDate.now()
        val workoutDates = workoutDocs
            .mapNotNull { it.getString("date") }
            .map { LocalDate.parse(it) }
            .sorted()
            .reversed()

        if (workoutDates.isEmpty()) {
            onComplete(0)
            return
        }

        // Check if worked out today or yesterday to continue streak
        val lastWorkout = workoutDates.first()
        if (lastWorkout.isBefore(today.minusDays(1))) {
            onComplete(0)
            return
        }

        // Calculate streak
        var currentDate = workoutDates.first()
        for (date in workoutDates) {
            if (date == currentDate || date == currentDate.minusDays(1)) {
                streak++
                currentDate = date
            } else {
                break
            }
        }

        onComplete(streak)
    }


    private fun showNotificationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notifications")
            .setMessage("Would you like to receive workout reminders?")
            .setPositiveButton("Enable") { _, _ ->
                // TODO: Implement basic notification permission request
                showToast("Notifications will be available in the next update!")
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage("""
            Privacy and Security:
            
            • We only collect essential data needed for your workouts
            • Your personal information is securely stored
            • We never share your data with third parties
            • You can delete your account and data at any time
            • We use Google Authentication for secure sign-in
            
        """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupClickListeners() {
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
        profileImage.setOnClickListener {
            openImagePicker()
        }

        notificationSettingsButton.setOnClickListener {
            showNotificationDialog()
        }

        privacyButton.setOnClickListener {
            showPrivacyPolicy()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            showLoading(true)

            // Delete profile image from internal storage
            deleteProfileImage()

            // Sign out from Firebase
            auth.signOut()

            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener {
                showLoading(false)
                // Navigate to SignIn screen
                startActivity(Intent(requireContext(), SignInActivity::class.java))
                requireActivity().finish()
            }
        } catch (e: Exception) {
            showLoading(false)
            showError("Failed to logout: ${e.message}")
        }
    }

    private fun deleteProfileImage() {
        try {
            val fileName = PROFILE_IMAGE_NAME
            val file = requireContext().getFileStreamPath(fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Failed to delete profile image: ${e.message}")
        }
    }

    private fun clearUserData() {
        // Clear SharedPreferences
        requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // TODO: Clear any other stored data (database, files, etc.)
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage() // Refresh image when returning to fragment
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}