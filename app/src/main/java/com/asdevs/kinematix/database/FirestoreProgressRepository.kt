package com.asdevs.kinematix.database

import android.content.ContentValues.TAG
import android.util.Log
import com.asdevs.kinematix.models.Progress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveProgress(progress: Progress, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val progressWithTimestamp = if (progress.timestamp == 0L) {
            progress.copy(timestamp = System.currentTimeMillis())
        } else progress

        db.collection("users")
            .document(userId)
            .collection("progress")
            .document(progress.date)
            .set(progressWithTimestamp)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                    exception ->
                Log.e(TAG, "Error saving progress: ${exception.message}")
                onError(exception)
            }
    }

    fun getProgress(date: String, onSuccess: (Progress) -> Unit, onError: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("progress")
            .document(date)
            .get()
            .addOnSuccessListener { document ->
                val progress = document.toObject(Progress::class.java) ?: Progress(
                    userId = userId,
                    date = date,
                    timestamp = System.currentTimeMillis()
                )
                onSuccess(progress)
            }
            .addOnFailureListener {
                    exception ->
                Log.e(TAG, "Error loading progress: ${exception.message}")
                onError(exception)
            }
    }
}