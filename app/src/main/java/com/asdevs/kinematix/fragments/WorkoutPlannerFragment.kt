package com.asdevs.kinematix.fragments

import com.asdevs.kinematix.database.FirestoreWorkoutRepository
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.UserProfileManager
import com.asdevs.kinematix.models.Exercise
import com.asdevs.kinematix.models.Workout
import com.asdevs.kinematix.workoutPlanner.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class WorkoutPlannerFragment : Fragment() {
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var repository: FirestoreWorkoutRepository
    private var isDataLoaded = false
    private var currentProfile: UserProfile? = null
    private var loadingOverlay: View? = null
    private var saveButton: MaterialButton? = null
    private var isDirty = false
    private val userProfileManager = UserProfileManager.getInstance()
    private val weekDays = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_planner, container, false)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        saveButton = view.findViewById(R.id.saveWorkoutButton)

        repository = FirestoreWorkoutRepository()
        setupRecyclerView(view)
        setupButtons(view)
        loadWorkouts()

        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvWorkouts)
        workoutAdapter = WorkoutAdapter(
            weekDays = weekDays,
            onWorkoutClick = { position -> showWorkoutDetails(position) },
            onEditClick = { position -> editWorkout(position) },
            onDeleteClick = { position -> deleteWorkout(position) }
        )
        recyclerView.apply {
            adapter = workoutAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialCardView>(R.id.btnCreateWorkout).setOnClickListener {
            showProfileSetupDialog()
        }

        view.findViewById<MaterialCardView>(R.id.btnAddWorkout).setOnClickListener {
            showAddWorkoutDialog()
        }

        saveButton = view.findViewById<MaterialButton>(R.id.saveWorkoutButton).apply {
            setOnClickListener {
                saveWorkouts()
            }
            isEnabled = false
            // Set initial state
            setIconResource(R.drawable.ic_save)
            text = "Save"
        }
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay?.apply {
            visibility = if (show) View.VISIBLE else View.GONE

            if (show) {
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            } else {
                animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        visibility = View.GONE
                    }
                    .start()
            }
        }
        view?.apply {
            findViewById<MaterialCardView>(R.id.btnCreateWorkout)?.isEnabled = !show
            findViewById<MaterialCardView>(R.id.btnAddWorkout)?.isEnabled = !show
            findViewById<MaterialButton>(R.id.saveWorkoutButton)?.isEnabled = !show
        }
    }

    private fun showProfileSetupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_profile_setup, null)

        setupSpinners(dialogView)
        loadUserProfile(dialogView)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Setup Your Profile")
            .setView(dialogView)
            .setPositiveButton("Create Workout Plan", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val profile = validateProfile(dialogView)
                if (profile != null) {
                    currentProfile = profile
                    generateWorkoutPlan(profile)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun loadUserProfile(dialogView: View) {
        showLoading(true)
        userProfileManager.getUserProfile(
            onSuccess = { userProfile ->
                showLoading(false)
                dialogView.apply {
                    findViewById<TextInputEditText>(R.id.etHeight)?.setText(userProfile.height.toString())
                    findViewById<TextInputEditText>(R.id.etWeight)?.setText(userProfile.weight.toString())
                    findViewById<TextInputEditText>(R.id.etAge)?.setText(userProfile.age.toString())

                    val genderSpinner = findViewById<Spinner>(R.id.spinnerGender)
                    val adapter = genderSpinner.adapter as ArrayAdapter<String>
                    val position = (0 until adapter.count).firstOrNull {
                        adapter.getItem(it) == userProfile.gender
                    } ?: 0
                    genderSpinner.setSelection(position)
                }
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to load profile: ${e.message}")
            }
        )
    }

    private fun setupSpinners(dialogView: View) {
        val context = requireContext()

        // Create adapters with custom layouts
        val genderAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.gender_options,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val fitnessLevelAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.fitness_levels,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val goalAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.fitness_goals,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val locationAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.workout_locations,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Apply adapters to spinners
        dialogView.apply {
            findViewById<Spinner>(R.id.spinnerGender).adapter = genderAdapter
            findViewById<Spinner>(R.id.spinnerFitnessLevel).adapter = fitnessLevelAdapter
            findViewById<Spinner>(R.id.spinnerGoal).adapter = goalAdapter
            findViewById<Spinner>(R.id.spinnerWorkoutLocation).adapter = locationAdapter
        }
    }


    private fun validateProfile(dialogView: View): UserProfile? {
        try {
            val height = dialogView.findViewById<TextInputEditText>(R.id.etHeight)
                .text.toString().toDoubleOrNull()
            val weight = dialogView.findViewById<TextInputEditText>(R.id.etWeight)
                .text.toString().toDoubleOrNull()
            val age = dialogView.findViewById<TextInputEditText>(R.id.etAge)
                .text.toString().toIntOrNull()
            val daysPerWeek = dialogView.findViewById<TextInputEditText>(R.id.etDaysPerWeek)
                .text.toString().toIntOrNull()

            val genderSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGender)
            val fitnessLevelSpinner = dialogView.findViewById<Spinner>(R.id.spinnerFitnessLevel)
            val goalSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGoal)
            val locationSpinner = dialogView.findViewById<Spinner>(R.id.spinnerWorkoutLocation)

            if (height == null || weight == null || age == null || daysPerWeek == null ||
                genderSpinner.selectedItemPosition == 0 ||
                fitnessLevelSpinner.selectedItemPosition == 0 ||
                goalSpinner.selectedItemPosition == 0 ||
                locationSpinner.selectedItemPosition == 0
            ) {
                showError("Please fill all fields")
                return null
            }

            if (daysPerWeek !in 1..7) {
                showError("Please enter valid number of workout days (1-7)")
                return null
            }

            return UserProfile(
                height = height,
                weight = weight,
                age = age,
                gender = genderSpinner.selectedItem.toString(),
                fitnessLevel = fitnessLevelSpinner.selectedItem.toString(),
                goal = goalSpinner.selectedItem.toString(),
                workoutDaysPerWeek = daysPerWeek,
                workoutLocation = locationSpinner.selectedItem.toString()
            )
        } catch (e: Exception) {
            showError("Error creating profile: ${e.message}")
            return null
        }
    }

    private fun generateWorkoutPlan(profile: UserProfile) {
        showLoading(true)
        try {
            val recommender = WorkoutRecommender()
            val workouts = recommender.recommendWorkout(profile)

            val sortedWorkouts = workouts.sortedBy { workout ->
                weekDays.indexOf(workout.date)
            }

            workoutAdapter.updateWorkouts(sortedWorkouts)
            markUnsavedChanges()
            showSuccess("Workout plan generated successfully! Don't forget to save it.")
        } catch (e: Exception) {
            showError("Failed to generate workout plan: ${e.message}")
        } finally {
            showLoading(false)
        }
    }

    private fun editWorkout(position: Int) {
        val workout = workoutAdapter.getWorkouts()[position]
        showEditExercisesDialog(position, workout.exercises)
        markUnsavedChanges()
    }

    private fun showEditExercisesDialog(workoutPosition: Int, exercises: List<Exercise>) {
        val exercisesList = exercises.toMutableList()

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Edit Exercises")
            .setPositiveButton("Save") { _, _ ->
                val currentWorkouts = workoutAdapter.getWorkouts().toMutableList()
                currentWorkouts[workoutPosition] = currentWorkouts[workoutPosition].copy(exercises = exercisesList)
                workoutAdapter.updateWorkouts(currentWorkouts)
                showSuccess("Exercises updated successfully")
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Add Exercise") { dialog, _ ->
                showEditExerciseDialog(exercisesList) {
                    dialog.dismiss()
                    showEditExercisesDialog(workoutPosition, exercisesList)
                }
            }
            .setAdapter(
                ExercisesDialogAdapter(requireContext(), exercisesList) { position ->
                    showEditExerciseDialog(position, exercisesList[position]) { updatedExercise ->
                        exercisesList[position] = updatedExercise
                    }
                },
                null
            )
            .show()
    }

    private fun showEditExerciseDialog(position: Int, exercise: Exercise, onExerciseUpdated: (Exercise) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_exercise, null)

        dialogView.apply {
            findViewById<TextInputEditText>(R.id.etExerciseName).setText(exercise.name)
            findViewById<TextInputEditText>(R.id.etSets).setText(exercise.sets.toString())
            findViewById<TextInputEditText>(R.id.etReps).setText(exercise.reps)
            findViewById<TextInputEditText>(R.id.etWeight).setText(exercise.weight)
            findViewById<TextInputEditText>(R.id.etRest).setText(exercise.rest)
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Edit Exercise")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedExercise = Exercise(
                    name = dialogView.findViewById<TextInputEditText>(R.id.etExerciseName).text.toString(),
                    sets = dialogView.findViewById<TextInputEditText>(R.id.etSets).text.toString().toIntOrNull() ?: 0,
                    reps = dialogView.findViewById<TextInputEditText>(R.id.etReps).text.toString(),
                    weight = dialogView.findViewById<TextInputEditText>(R.id.etWeight).text.toString(),
                    rest = dialogView.findViewById<TextInputEditText>(R.id.etRest).text.toString()
                )
                onExerciseUpdated(updatedExercise)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditExerciseDialog(exercisesList: MutableList<Exercise>, onComplete: () -> Unit) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_exercise, null)

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Add Exercise")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newExercise = Exercise(
                    name = dialogView.findViewById<TextInputEditText>(R.id.etExerciseName).text.toString(),
                    sets = dialogView.findViewById<TextInputEditText>(R.id.etSets).text.toString().toIntOrNull() ?: 0,
                    reps = dialogView.findViewById<TextInputEditText>(R.id.etReps).text.toString(),
                    weight = dialogView.findViewById<TextInputEditText>(R.id.etWeight).text.toString(),
                    rest = dialogView.findViewById<TextInputEditText>(R.id.etRest).text.toString()
                )
                exercisesList.add(newExercise)
                onComplete()
            }
            .setNegativeButton("Cancel") { _, _ -> onComplete() }
            .show()
    }

    private fun deleteWorkout(position: Int) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete this workout?")
            .setPositiveButton("Delete") { _, _ ->
                val currentWorkouts = workoutAdapter.getWorkouts().toMutableList()
                currentWorkouts.removeAt(position)
                workoutAdapter.updateWorkouts(currentWorkouts)
                markUnsavedChanges()
                showSuccess("Workout deleted successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWorkoutDetails(position: Int) {
        val workout = workoutAdapter.getWorkouts()[position]
        val message = buildString {
            appendLine("${workout.date}'s Workout")
            appendLine("\nExercises:")
            workout.exercises.forEachIndexed { index, exercise ->
                appendLine("\n${index + 1}. ${exercise.name}")
                appendLine("   Sets: ${exercise.sets}")
                appendLine("   Reps: ${exercise.reps}")
                if (exercise.weight.isNotBlank()) appendLine("   Weight: ${exercise.weight}")
                if (exercise.rest.isNotBlank()) appendLine("   Rest: ${exercise.rest}")
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Workout Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }


    private fun showAddWorkoutDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_workout, null)

        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            weekDays
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        spinner.adapter = adapter

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Add New Workout")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                if (spinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
                    showError("Please select a day")
                    return@setPositiveButton
                }

                val selectedDay = weekDays[spinner.selectedItemPosition]

                if (workoutAdapter.getWorkouts().any { it.date == selectedDay }) {
                    showError("Workout for $selectedDay already exists")
                    return@setPositiveButton
                }

                val newWorkout = Workout(
                    date = selectedDay,
                    exercises = mutableListOf()
                )

                val currentWorkouts = workoutAdapter.getWorkouts().toMutableList()

                val insertIndex = findInsertIndex(currentWorkouts, selectedDay)
                currentWorkouts.add(insertIndex, newWorkout)
                workoutAdapter.updateWorkouts(currentWorkouts)

                // Show exercise editing dialog for the new workout
                editWorkout(insertIndex)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun markUnsavedChanges() {
        isDirty = true
        saveButton?.apply {
            isEnabled = true
            setIconResource(R.drawable.ic_save)
            text = "Save"
        }
    }

    private fun onWorkoutsModified() {
        markUnsavedChanges()
    }

    private fun findInsertIndex(workouts: List<Workout>, newDay: String): Int {
        val newDayIndex = weekDays.indexOf(newDay)
        return workouts.indexOfFirst { workout ->
            val workoutDayIndex = weekDays.indexOf(workout.date)
            workoutDayIndex > newDayIndex
        }.let { if (it == -1) workouts.size else it }
    }

    private fun loadWorkouts() {
        showLoading(true)
        repository.getWorkoutRoutine(
            onSuccess = { workouts ->
                showLoading(false)
                isDataLoaded = true
                workoutAdapter.updateWorkouts(workouts)
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to load workouts: ${e.message}")
            }
        )
    }

    private fun saveWorkouts() {
        val workouts = workoutAdapter.getWorkouts()
        if (workouts.isEmpty()) {
            showError("No workouts to save")
            return
        }
        showLoading(true)

        repository.saveWorkoutRoutine(
            workouts = workouts,
            onSuccess = {
                showLoading(false)
                showSuccess("Workouts saved successfully")
                isDirty = false
                saveButton?.apply {
                    isEnabled = false
                    setIconResource(R.drawable.ic_check)
                    text = "Saved"
                }
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to save workouts: ${e.message}")
            }
        )
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}