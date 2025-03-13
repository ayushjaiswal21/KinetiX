package com.asdevs.kinematix.workoutPlanner

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.asdevs.kinematix.R
import com.asdevs.kinematix.database.FirestoreWorkoutRepository
import com.asdevs.kinematix.models.Exercise
import com.asdevs.kinematix.models.Workout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkoutExerciseFragment : Fragment(R.layout.fragment_workout_exercise) {
    private var currentExercise = 0
    private var totalExercises = 0
    private var currentSet = 0
    private var totalSets = 0
    private var workout: Workout? = null
    private val workoutRepository = FirestoreWorkoutRepository()
    private lateinit var backPressCallback: OnBackPressedCallback
    private lateinit var videoView: VideoView
    private lateinit var exerciseImageView: ImageView

    interface WorkoutCompletionListener {
        fun onWorkoutCompleted()
    }

    private var workoutCompletionListener: WorkoutCompletionListener? = null

    fun setWorkoutCompletionListener(listener: WorkoutCompletionListener) {
        workoutCompletionListener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoView = view.findViewById(R.id.iv_exercise)
        exerciseImageView = view.findViewById(R.id.exercise_image)

        // Load workout from arguments
        arguments?.getString("day")?.let { day ->
            loadWorkout(day)
        }

        setupBackPress()
        setupClickListeners()
    }

    private fun setupBackPress() {
        backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressCallback
        )
    }

    private fun loadWorkout(day: String) {
        workoutRepository.getWorkoutRoutine(
            onSuccess = { workouts ->
                workout = workouts.find { it.date == day }
                workout?.let {
                    totalExercises = it.exercises.size
                    setupExercise()
                    setupClickListeners()
                } ?: run {
                    showErrorAndExit("No workout found for $day")
                }
            },
            onError = { exception ->
                showErrorAndExit("Failed to load workout: ${exception.message}")
            }
        )
    }

    private fun setupExercise() {
        val exercise = getCurrentExercise() ?: return
        Log.d("VideoDebug", "Setting up exercise: ${exercise?.name}")
        if (exercise == null) return

        view?.apply {
            // Update exercise name and other details
            val exerciseName = exercise.name
            Log.d("VideoDebug", "Exercise name before check: $exerciseName")
            // Update exercise name and other details
            findViewById<TextView>(R.id.tv_exercise_name)?.text = exercise.name
            findViewById<TextView>(R.id.tv_progress)?.text =
                "Exercise ${currentExercise + 1}/$totalExercises"

            // Check if video exists for this exercise
            val hasVideo = hasVideoForExercise(exercise.name)
            Log.d("VideoDebug", "Has video for ${exercise.name}: $hasVideo")

            if (hasVideo) {
                Log.d("VideoDebug", "Attempting to show video")
                // Show video, hide image
                videoView.apply {
                    visibility = View.VISIBLE
                    stopPlayback() // Clear any existing video
                    setupVideo(this)
                }
                exerciseImageView.visibility = View.GONE
                Log.d("VideoDebug", "Video view visibility set to VISIBLE")
            } else {
                Log.d("VideoDebug", "Showing image instead")
                // Show image, hide video
                videoView.apply {
                    stopPlayback() // Clear any existing video
                    visibility = View.GONE
                }
                exerciseImageView.apply {
                    visibility = View.VISIBLE
                    setupImage(this, exercise.name)
                }
                Log.d("VideoDebug", "Image view visibility set to VISIBLE")
            }

            // Update other UI elements
            findViewById<TextView>(R.id.tv_sets)?.text = "${exercise.sets} Sets"
            findViewById<TextView>(R.id.tv_reps)?.text = "${exercise.reps}"

            currentSet = 0
            totalSets = exercise.sets
            updateSetsProgress()

            // Update complete set button text
            findViewById<MaterialButton>(R.id.btn_complete_set)?.apply {
                isEnabled = true
                text = "Complete Set 1"
            }
        }
    }

    private fun hasVideoForExercise(exerciseName: String): Boolean {
        val normalizedName = exerciseName.lowercase().trim()
        Log.d("VideoDebug", "Checking video for normalized name: $normalizedName")

        return when (normalizedName) {
            "bench press", "flat bench press" -> true
            "burpees" -> true
            "calf raises" -> true
            "close grip bench press", "close grip bench" -> true
            "elliptical" -> true
            "front raises" -> true
            "incline bench press", "inclined dumbbell press", "incline dumbbell press" -> true
            "leg curl" -> true
            "leg raises" -> true
            "lunges", "walking lunges" -> true
            "plank" -> true
            "push-ups", "pushups" -> true
            "shoulder dumbbell press" -> true
            "squat jumps" -> true
            "squats" -> true
            "treadmill" -> true
            "barbell rows" -> true
            "bicep curl" -> true
            "tricep extension", "overhead tricep extension", "pulley push down" -> true
            else -> {
                Log.d("VideoDebug", "No video mapping found for: $normalizedName")
                false
            }
        }
    }

    private fun setupVideo(videoView: VideoView) {
        try {
            val exercise = getCurrentExercise()?.name?.lowercase()?.trim() ?: return
            Log.d("VideoDebug", "Setting up video for: $exercise")

            val videoResId = when (exercise) {
                "bench press", "flat bench press" -> R.raw.benchpress_vd
                "burpees" -> R.raw.burpees_vd
                "calf raises" -> R.raw.calf_raises_vd
                "close grip bench press", "close grip bench" -> R.raw.close_bench_vd
                "elliptical" -> R.raw.elliptical_vd
                "front raises" -> R.raw.front_raises_vd
                "incline bench press", "inclined dumbbell press", "incline dumbbell press" -> R.raw.incline_bench_vd
                "leg curl" -> R.raw.leg_curl_vd
                "leg raises" -> R.raw.leg_raises_vd
                "lunges", "walking lunges" -> R.raw.lunges_vd
                "plank" -> R.raw.plank_vd
                "push-ups", "pushups" -> R.raw.pushups_vd
                "shoulder dumbbell press" -> R.raw.shoulder_dumbbell_vd
                "squat jumps" -> R.raw.squat_jumps_vd
                "squats" -> R.raw.squat_vd
                "treadmill" -> R.raw.treadmill_vd
                "barbell rows" -> R.raw.barbell_rows_vd
                "dumbbell curl" -> R.raw.bicep_curl_vd
                "hammer curl" -> R.raw.hammer_curl_vd
                "preacher curl" -> R.raw.preacher_curl_vd
                "tricep extension", "overhead tricep extension", "pulley push down" -> R.raw.tricep_ext_vd
                else -> null
            }

            if (videoResId == null) {
                Log.e("VideoDebug", "No video resource found for: $exercise")
                fallbackToImage()
                return
            }

            val path = "android.resource://${requireContext().packageName}/$videoResId"
            Log.d("VideoDebug", "Setting video path: $path")

            videoView.setVideoPath(path)
            videoView.setOnPreparedListener { mediaPlayer ->
                Log.d("VideoDebug", "Video prepared successfully")
                mediaPlayer.isLooping = true
                videoView.start()
            }

            videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoDebug", "Error playing video: what=$what extra=$extra")
                fallbackToImage()
                true
            }

            videoView.start()

        } catch (e: Exception) {
            Log.e("VideoDebug", "Error in setupVideo", e)
            fallbackToImage()
        }
    }

    private fun setupImage(imageView: ImageView, exerciseName: String) {
        val imageResId = when (exerciseName.lowercase().trim()) {
            // Bodyweight Exercises
            "push-ups", "push ups" -> R.drawable.pushups
            "squats" -> R.drawable.squats
            "lunges", "walking lunges" -> R.drawable.lunges
            "plank" -> R.drawable.plank
            "mountain climbers" -> R.drawable.mountain_climbers_img
            "burpees" -> R.drawable.burpees
            "glute bridges" -> R.drawable.glute_bridges
            "leg raises" -> R.drawable.leg_raises
            "diamond push-ups" -> R.drawable.diamond_pushups
            "pike push-ups" -> R.drawable.pike_pushups
            "dips", "tricep dips" -> R.drawable.dips
            "reverse plank hold" -> R.drawable.reverse_plank_hold
            "calf raises" -> R.drawable.calf_raises
            "russian twists" -> R.drawable.russian_twists

            // Gym Upper Body
            "bench press" -> R.drawable.benchpress
            "incline dumbbell press" -> R.drawable.incline_benchpress
            "cable cross" -> R.drawable.cable_cross_img
            "shoulder dumbbell press" -> R.drawable.benchpress
            "cable side raise" -> R.drawable.side_raises
            "face pull" -> R.drawable.face_pull_img
            "lat pulldown" -> R.drawable.lat_pulldown
            "seated rowing" -> R.drawable.seated_rowing
            "barbell rows" -> R.drawable.barbell_rows
            "dumbbell curls" -> R.drawable.hammer_curl
            "hammer curls" -> R.drawable.hammer_curl
            "preacher curls" -> R.drawable.preacher_curl
            "pulley push down" -> R.drawable.pully_pushdown_img
            "overhead tricep extension" -> R.drawable.overhead_tricep
            "close grip bench" -> R.drawable.closegrip_bench

            // Gym Lower Body
            "deadlifts" -> R.drawable.deadlift
            "leg press" -> R.drawable.leg_press
            "leg curl" -> R.drawable.leg_curl

            // Yoga Poses
            "sun salutation (सूर्य नमस्कार)", "सूर्य नमस्कार", "sun salutation" -> R.drawable.warrior1_yg
            "cat-cow stretch (मार्जरीआसन-बिटिलासन)", "मार्जरीआसन-बिटिलासन", "cat-cow stretch" -> R.drawable.cat_cow_yg
            "downward dog (अधोमुख श्वानासन)", "अधोमुख श्वानासन", "downward dog" -> R.drawable.downward_dog_yg
            "warrior i (वीरभद्रासन)", "वीरभद्रासन", "warrior i", "warrior 1" -> R.drawable.warrior1_yg
            "warrior ii", "warrior 2", "warrior 2 (वीरभद्रासन द्वितीय)", "वीरभद्रासन द्वितीय" -> R.drawable.warrior2_yg
            "tree pose (वृक्षासन)", "वृक्षासन", "tree pose" -> R.drawable.tree_pose_yg
            "child's pose (बालासन)", "बालासन", "child's pose", "childs pose" -> R.drawable.child_pose_yg
            "triangle pose (त्रिकोणासन)", "त्रिकोणासन", "triangle pose" -> R.drawable.triangle_pose_yg
            "crow pose (बकासन)", "बकासन", "crow pose" -> R.drawable.crow_pose_yg
            "bridge pose (सेतुबंधासन)", "सेतुबंधासन", "bridge pose" -> R.drawable.bridge_pose_yg

            // Default fallback
            else -> R.drawable.warrior1_yg
        }

        try {
            Glide.with(requireContext())
                .load(imageResId)
                .transition(DrawableTransitionOptions.withCrossFade())
                .transform(CenterCrop())
                .error(R.drawable.deadlift) // Using deadlift as fallback image
                .into(imageView)
        } catch (e: Exception) {
            Log.e("ImageDebug", "Error loading image for $exerciseName", e)
            // Load default image if there's an error
            Glide.with(requireContext())
                .load(R.drawable.deadlift)
                .into(imageView)
        }
    }

    private fun fallbackToImage() {
        Log.d("VideoDebug", "Falling back to image")
        videoView.apply {
            stopPlayback()
            visibility = View.GONE
        }
        exerciseImageView.apply {
            visibility = View.VISIBLE
            getCurrentExercise()?.name?.let { exerciseName ->
                setupImage(this, exerciseName)
            }
        }
    }


    private fun setupClickListeners() {
        view?.apply {
            // Complete set button
            findViewById<MaterialButton>(R.id.btn_complete_set)?.setOnClickListener {
                completeSet()
            }

            // Skip button
            findViewById<ImageButton>(R.id.btn_skip)?.setOnClickListener {
                showSkipConfirmation()
            }

            // Close button
            findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
                showExitConfirmation()
            }
        }
    }

    private fun getCurrentExercise(): Exercise? {
        return workout?.exercises?.getOrNull(currentExercise)
    }

    private fun completeSet() {
        currentSet++
        if (currentSet >= totalSets) {
            // Move to next exercise
            completeExercise()
        } else {
            // Update progress and start rest timer
            updateSetsProgress()
            view?.findViewById<MaterialButton>(R.id.btn_complete_set)?.apply {
                isEnabled = true
                text = "Complete Set ${currentSet + 1}"
            }
        }
    }

    private fun updateSetsProgress() {
        view?.findViewById<TextView>(R.id.tv_sets_completed)?.text =
            "$currentSet/$totalSets"
    }

    private fun completeExercise() {
        Log.d("WorkoutDebug", "Completing exercise ${currentExercise + 1}/$totalExercises")

        if (currentExercise + 1 >= totalExercises) {
            // This was the last exercise
            Log.d("WorkoutDebug", "Last exercise completed, completing workout")
            completeWorkout()
        } else {
            moveToNextExercise()
        }
    }

    private fun completeWorkout() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().time)

        Log.d("WorkoutDebug", "Starting workout completion process")

        // Calculate workout duration
        val duration = calculateWorkoutDuration()
        val exercisesCompleted = totalExercises

        workoutRepository.saveCompletedWorkout(
            date = today,
            onSuccess = {
                Log.d("WorkoutDebug", "Workout saved as completed")
                // Update streak after successful completion
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    updateStreak(today)
                }

                // Save workout progress
                workoutRepository.saveWorkoutProgress(
                    day = today,
                    duration = duration,
                    exercisesCompleted = exercisesCompleted,
                    onSuccess = {
                        Log.d("WorkoutDebug", "Workout progress saved")
                        requireActivity().runOnUiThread {
                            showWorkoutCompleteDialog()
                        }
                    },
                    onError = { exception ->
                        Log.e("WorkoutDebug", "Error saving workout progress: ${exception.message}")
                        showErrorMessage("Failed to save workout progress")
                    }
                )
            },
            onError = { exception ->
                Log.e("WorkoutDebug", "Error completing workout: ${exception.message}")
                showErrorMessage("Failed to complete workout")
            }
        )
    }

    private fun moveToNextExercise() {
        currentExercise++
        if (currentExercise >= totalExercises) {
            // Workout completed
            showWorkoutCompleteDialog()
        } else {
            // Reset sets and load next exercise
            currentSet = 0
            setupExercise()
        }
    }

    private fun showWorkoutCompleteDialog() {
        if (!isAdded) return

        val duration = formatDuration(calculateWorkoutDuration())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Workout Complete! 💪")
            .setMessage("Great job!\n\nWorkout Duration: $duration\nExercises Completed: $totalExercises")
            .setPositiveButton("Finish") { dialog, _ ->
                Log.d("WorkoutDebug", "User confirmed workout completion")
                try {
                    // First dismiss the dialog
                    dialog.dismiss()

                    // Then notify completion
                    workoutCompletionListener?.onWorkoutCompleted()

                    // Finally navigate back to home
                    if (isAdded) {
                        requireActivity().runOnUiThread {
                            parentFragmentManager.popBackStack()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutDebug", "Error in workout completion: ${e.message}")
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showSkipConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Skip Exercise")
            .setMessage("Are you sure you want to skip this exercise?")
            .setPositiveButton("Skip") { _, _ ->
                moveToNextExercise()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exit Workout")
            .setMessage("Are you sure you want to end this workout? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                exitWorkout()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun showErrorAndExit(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun exitWorkout() {
        try {
            Log.d("WorkoutDebug", "Exiting workout")
            if (isAdded) {  // Check if fragment is still attached
                requireActivity().supportFragmentManager.popBackStack()
            }
        } catch (e: Exception) {
            Log.e("WorkoutDebug", "Error exiting workout: ${e.message}")
        }
    }

    private fun updateStreak(date: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        workoutRepository.updateStreak(
            userId = userId,
            day = date,
            onSuccess = { newStreak ->
                Log.d("WorkoutDebug", "Streak updated successfully: $newStreak days")
            },
            onError = { exception ->
                Log.e("WorkoutDebug", "Failed to update streak: ${exception.message}")
                // Don't show error to user as this is not critical
            }
        )
    }

    private fun calculateWorkoutDuration(): Long {
        val startTime = arguments?.getLong("startTime") ?: return 0
        return System.currentTimeMillis() - startTime
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 1000 / 60
        return "$minutes minutes"
    }

    override fun onPause() {
        super.onPause()
        backPressCallback.remove()
        if (::videoView.isInitialized) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        setupBackPress()
        if (::videoView.isInitialized) {
            videoView.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::videoView.isInitialized) {
            videoView.stopPlayback()
        }
    }
}