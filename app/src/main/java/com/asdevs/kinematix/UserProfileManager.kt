package com.asdevs.kinematix

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUserProfile(onSuccess: (UserProfile) -> Unit, onError: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onError(Exception("User not logged in"))

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val profile = UserProfile(
                        name = document.getString("name") ?: "",
                        height = document.getDouble("height")?.toFloat() ?: 0f,
                        weight = document.getDouble("weight")?.toFloat() ?: 0f,
                        age = document.getLong("age")?.toInt() ?: 0,
                        gender = document.getString("gender") ?: "Other",
                        email = document.getString("email") ?: "",
                        photoUrl = document.getString("photoUrl") ?: ""
                    )
                    onSuccess(profile)
                } else {
                    onError(Exception("Profile not found"))
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    data class UserProfile(
        val name: String,
        val height: Float,
        val weight: Float,
        val age: Int,
        val gender: String,
        val email: String,
        val photoUrl: String
    )

    companion object {
        @Volatile
        private var instance: UserProfileManager? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: UserProfileManager().also { instance = it }
        }
    }
}