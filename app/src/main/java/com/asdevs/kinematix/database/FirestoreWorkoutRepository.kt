package com.asdevs.kinematix.database

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.asdevs.kinematix.models.Exercise
import com.asdevs.kinematix.models.Workout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FirestoreWorkoutRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workoutsCollection = db.collection("workouts")
    private val streakCollection = db.collection("streaks")

    private val weekDays = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    fun saveCompletedWorkout(
        date: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            Log.d("WorkoutDebug", "Starting saveCompletedWorkout for date: $date")
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

            val workoutData = hashMapOf(
                "date" to date,
                "completed" to true,
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId
            )

            // First, check if this workout was already completed
            db.collection("users")
                .document(userId)
                .collection("completed_workouts")
                .document(date)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        // Only increment workout count if this is a new completion
                        incrementTotalWorkouts(userId)
                    }

                    // Save the workout completion
                    db.collection("users")
                        .document(userId)
                        .collection("completed_workouts")
                        .document(date)
                        .set(workoutData)
                        .addOnSuccessListener {
                            Log.d("WorkoutDebug", "Successfully saved completed workout")
                            updateStreak(
                                userId = userId,
                                day = date,
                                onSuccess = { streak ->
                                    Log.d("WorkoutDebug", "Updated streak to: $streak")
                                    onSuccess()
                                },
                                onError = { exception ->
                                    onError(exception)
                                }
                            )
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (exception: Exception) {
            Log.e("WorkoutDebug", "Error in saveCompletedWorkout: ${exception.message}")
            onError(exception)
        }
    }

    private fun incrementTotalWorkouts(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Use transaction to ensure atomic update
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(db.collection("users").document(userId))
                    val currentTotal = snapshot.getLong("totalWorkouts") ?: 0
                    transaction.update(db.collection("users").document(userId),
                        "totalWorkouts", currentTotal + 1)
                }.addOnFailureListener { e ->
                    Log.e("WorkoutDebug", "Error updating total workouts: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutDebug", "Error getting current total workouts: ${e.message}")
            }
    }

    fun getUserStats(
        userId: String,
        onSuccess: (totalWorkouts: Long, currentStreak: Long, bestStreak: Long) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Get both user document and streak document
        val userRef = db.collection("users").document(userId)
        val streakRef = streakCollection.document(userId)

        // Use Promise-like pattern to get both documents
        userRef.get().addOnSuccessListener { userDoc ->
            streakRef.get().addOnSuccessListener { streakDoc ->
                val totalWorkouts = userDoc.getLong("totalWorkouts") ?: 0
                val currentStreak = streakDoc.getLong("currentStreak") ?: 0
                val bestStreak = streakDoc.getLong("bestStreak") ?: 0
                onSuccess(totalWorkouts, currentStreak, bestStreak)
            }.addOnFailureListener { exception ->
                onError(exception)
            }
        }.addOnFailureListener { exception ->
            onError(exception)
        }
    }

    fun updateWorkoutStats(
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Get the user's document reference
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            // Get current stats
            val userDoc = transaction.get(userRef)
            val currentWorkouts = userDoc.getLong("totalWorkouts") ?: 0

            // Update the stats
            transaction.update(userRef,
                "totalWorkouts", currentWorkouts + 1
            )
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(exception)
        }
    }

    fun updateStreak(
        userId: String,
        day: String,
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First check if streak should be broken
        checkForStreakBreak(userId)

        streakCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                val currentStreak = document?.getLong("currentStreak") ?: 0
                val lastWorkoutDate = document?.getString("lastWorkoutDate")
                val bestStreak = document?.getLong("bestStreak") ?: 0

                // Check if this workout is on a consecutive day
                val isConsecutiveDay = isConsecutiveDay(lastWorkoutDate, day)

                // Calculate new streak
                val newStreak = when {
                    isConsecutiveDay -> currentStreak + 1
                    lastWorkoutDate == day -> currentStreak  // Same day workout
                    else -> 1  // Break in streak
                }
                val newBestStreak = maxOf(bestStreak, newStreak)

                // Update streak data
                streakCollection.document(userId)
                    .set(
                        mapOf(
                            "currentStreak" to newStreak,
                            "lastWorkoutDate" to day,
                            "bestStreak" to newBestStreak
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        onSuccess(newStreak.toInt())
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun isWorkoutCompletedForDate(
        date: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d("WorkoutDebug", "Checking completion for date: $date")

        db.collection("users")
            .document(userId)
            .collection("completed_workouts")
            .document(date)
            .get()
            .addOnSuccessListener { document ->
                val isCompleted = document.exists() && document.getBoolean("completed") == true
                Log.d("WorkoutDebug", "Workout completion status for $date: $isCompleted")
                onSuccess(isCompleted)
            }
            .addOnFailureListener { exception ->
                Log.e("WorkoutDebug", "Error checking workout completion: ${exception.message}")
                onError(exception)
            }
    }


    private fun isConsecutiveDay(lastWorkoutDate: String?, currentDate: String): Boolean {
        if (lastWorkoutDate == null) return false  // First workout should start streak at 1

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val last = dateFormat.parse(lastWorkoutDate)
            val current = dateFormat.parse(currentDate)

            if (last != null && current != null) {
                val diff = current.time - last.time
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)


                return daysDiff == 0L || daysDiff == 1L
            }
        } catch (e: Exception) {
            Log.e("WorkoutDebug", "Error parsing dates: ${e.message}")
        }
        return false
    }

    private fun checkForStreakBreak(userId: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)  // Check yesterday
        val yesterday = dateFormat.format(calendar.time)

        isWorkoutCompletedForDate(
            date = yesterday,
            onSuccess = { completed ->
                if (!completed) {
                    streakCollection.document(userId)
                        .update("currentStreak", 0)
                        .addOnFailureListener { e ->
                            Log.e("WorkoutDebug", "Error resetting streak: ${e.message}")
                        }
                }
            },
            onError = { e ->
                Log.e("WorkoutDebug", "Error checking yesterday's workout: ${e.message}")
            }
        )
    }

    fun getWorkoutRoutine(
        onSuccess: (List<Workout>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            db.collection("users")
                .document(userId)
                .collection("workouts")
                .document("routine")
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document != null && document.exists()) {
                            @Suppress("UNCHECKED_CAST")
                            val workoutsList = document.get("workouts") as? List<HashMap<String, Any>>

                            if (workoutsList != null) {
                                val workouts = workoutsList.mapNotNull { workoutMap ->
                                    parseWorkout(workoutMap)
                                }

                                val sortedWorkouts = workouts.sortedBy { workout ->
                                    weekDays.indexOf(workout.date)
                                }

                                println("Retrieved ${sortedWorkouts.size} workouts")
                                onSuccess(sortedWorkouts)
                            } else {
                                println("No workouts found")
                                onSuccess(emptyList())
                            }
                        } else {
                            println("No workout document found")
                            onSuccess(emptyList())
                        }
                    } catch (e: Exception) {
                        println("Error parsing workout data: ${e.message}")
                        onError(e)
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error retrieving workout routine: ${exception.message}")
                    onError(exception)
                }
        } catch (e: Exception) {
            println("Error in getWorkoutRoutine: ${e.message}")
            onError(e)
        }
    }

    private fun parseWorkout(workoutMap: HashMap<String, Any>): Workout? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val exercisesList = workoutMap["exercises"] as? List<HashMap<String, Any>>
                ?: return null

            Workout(
                date = workoutMap["date"] as? String ?: return null,
                exercises = exercisesList.mapNotNull { exerciseMap ->
                    parseExercise(exerciseMap)
                }
            )
        } catch (e: Exception) {
            println("Error parsing workout: ${e.message}")
            null
        }
    }

    private fun parseExercise(exerciseMap: HashMap<String, Any>): Exercise? {
        return try {
            Exercise(
                name = exerciseMap["name"] as? String ?: return null,
                sets = (exerciseMap["sets"] as? Long)?.toInt() ?: return null,
                reps = exerciseMap["reps"] as? String ?: return null,
                rest = exerciseMap["rest"] as? String ?: return null,
                weight = exerciseMap["weight"] as? String ?: return null,
                isCompleted = exerciseMap["isCompleted"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            println("Error parsing exercise: ${e.message}")
            null
        }
    }

    private fun validateWorkout(workout: Workout): Boolean {
        return workout.date in weekDays &&
                workout.exercises.isNotEmpty() &&
                workout.exercises.all { exercise ->
                    exercise.name.isNotEmpty() &&
                            exercise.sets > 0 &&
                            exercise.reps.isNotEmpty() &&
                            exercise.rest.isNotEmpty()
                }
    }

    fun saveWorkoutRoutine(
        workouts: List<Workout>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            Log.d("WorkoutDebug", "Saving workout routine with ${workouts.size} workouts")

            // Validate workouts before saving
            if (workouts.any { !validateWorkout(it) }) {
                onError(IllegalArgumentException("Invalid workout data"))
                return
            }

            // Convert workouts to map format
            val workoutData = workouts.map { workout ->
                hashMapOf(
                    "date" to workout.date,
                    "exercises" to workout.exercises.map { exercise ->
                        hashMapOf(
                            "name" to exercise.name,
                            "sets" to exercise.sets,
                            "reps" to exercise.reps,
                            "rest" to exercise.rest,
                            "weight" to exercise.weight,
                            "isCompleted" to exercise.isCompleted
                        )
                    }
                )
            }

            // Save to Firestore
            db.collection("users")
                .document(userId)
                .collection("workouts")
                .document("routine")
                .set(hashMapOf("workouts" to workoutData))
                .addOnSuccessListener {
                    Log.d("WorkoutDebug", "Workout routine saved successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("WorkoutDebug", "Error saving workout routine: ${exception.message}")
                    onError(exception)
                }
        } catch (exception: Exception) {
            Log.e("WorkoutDebug", "Error in saveWorkoutRoutine: ${exception.message}")
            onError(exception)
        }
    }

    fun getCompletedWorkouts(
        year: Int,
        month: Int,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val monthStr = String.format("%02d", month)
            val startDate = "$year-$monthStr-01"
            val endDate = "$year-$monthStr-31"

            Log.d("WorkoutDebug", "Fetching completed workouts")
            Log.d("WorkoutDebug", "User ID: $userId")
            Log.d("WorkoutDebug", "Date range: $startDate to $endDate")

            // Updated path to match Firestore rules
            db.collection("users")
                .document(userId)
                .collection("completed_workouts")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener { documents ->
                    val completedDates = documents
                        .filter { it.getBoolean("completed") == true }
                        .mapNotNull { it.getString("date") }
                    Log.d("WorkoutDebug", "Found ${completedDates.size} completed workouts")
                    onSuccess(completedDates)
                }
                .addOnFailureListener { exception ->
                    Log.e("WorkoutDebug", "Error getting completed workouts: ${exception.message}")
                    onError(exception)
                }
        } catch (exception: Exception) {
            Log.e("WorkoutDebug", "Error in getCompletedWorkouts: ${exception.message}")
            onError(exception)
        }
    }

    fun saveWorkoutProgress(
        day: String,
        duration: Long,
        exercisesCompleted: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val progressData = hashMapOf(
            "date" to day,
            "duration" to duration,
            "exercisesCompleted" to exercisesCompleted,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("workout_progress")
            .document(day)  // Use day as document ID
            .set(progressData)
            .addOnSuccessListener {
                Log.d("WorkoutDebug", "Workout progress saved successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("WorkoutDebug", "Error saving workout progress: ${exception.message}")
                onError(exception)
            }
    }
}