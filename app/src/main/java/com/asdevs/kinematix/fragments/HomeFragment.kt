package com.asdevs.kinematix.fragments

import com.asdevs.kinematix.database.FirestoreWorkoutRepository
import android.Manifest
import android.accounts.NetworkErrorException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.database.FirestoreNutritionRepository
import com.asdevs.kinematix.database.FirestoreProgressRepository
import com.asdevs.kinematix.models.Meal
import com.asdevs.kinematix.models.Workout
import com.asdevs.kinematix.models.Progress
import com.asdevs.kinematix.progress.StepCounter
import com.asdevs.kinematix.workoutPlanner.WorkoutExerciseFragment
import com.asdevs.kinematix.workoutStreak.StreakAdapter
import com.asdevs.kinematix.models.StreakItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var streakAdapter: StreakAdapter
    private val calendar = Calendar.getInstance()

    private val workoutRepository = FirestoreWorkoutRepository()
    private val nutritionRepository = FirestoreNutritionRepository()

    private lateinit var workoutCardView: View
    private lateinit var mealCardView: View
    private var isUpdatingWorkoutCard = false

    private var stepsGoal = 10000
    private var caloriesGoal = 2200
    private var waterGoal = 2.5f

    private var currentSteps = 0
    private var currentCalories = 0
    private var currentWater = 0f

    private val completedMeals = mutableSetOf<String>()
    private var currentMeals: List<Meal>? = null

    private lateinit var stepCounter: StepCounter

    private val progressRepository = FirestoreProgressRepository()
    private var today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    private lateinit var fabStartWorkout: FloatingActionButton
    private lateinit var profileImage: ShapeableImageView
    private val PROFILE_IMAGE_NAME = "profile_image.jpg"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupStreakRecyclerView()
        setupMonthNavigation()
        setupProgressCards()
        checkStepCounterPermission()
        checkStepSensorAvailability()
        updateMonthDisplay()
        updateStreakDates()
        updateProgress()
        setupProgressListeners()
        setupEditGoalsButton()

        workoutCardView = view.findViewById(R.id.card_today_workout)
        mealCardView = view.findViewById(R.id.card_current_meal)
        fabStartWorkout = view.findViewById(R.id.fab_start_workout)
        stepCounter = StepCounter(requireContext())
        stepCounter.setStepUpdateListener { steps ->
            currentSteps = steps
            updateProgress()
            saveCurrentProgress()
        }
        profileImage = view.findViewById(R.id.iv_profile)
        loadProfileImage()

        loadGoals()
        loadCurrentProgress()
        loadTodayWorkoutAndMeal()
        updateProgress()

        stepCounter = StepCounter(requireContext())
        stepCounter.setStepUpdateListener { steps ->
            if (steps > currentSteps) {  // Only update if new count is higher
                currentSteps = steps
                updateProgress()
                saveCurrentProgress()
            }
        }

    }



    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        FirebaseAuth.getInstance().currentUser?.let { user ->
            view?.findViewById<TextView>(R.id.tv_greeting)?.text = "$greeting,"
            // Get only the first name (text before first space)
            val firstName = user.displayName?.split(" ")?.firstOrNull() ?: "User"
            view?.findViewById<TextView>(R.id.tv_username)?.text = firstName
        }
    }

    fun loadProfileImage() {
        try {
            val fileName = PROFILE_IMAGE_NAME
            val file = requireContext().getFileStreamPath(fileName)

            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .skipMemoryCache(true) // Skip memory caching
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk caching
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage)
            } else {
                // If no profile image exists, load the default
                Glide.with(this)
                    .load(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to load profile image: ${e.message}")
        }
    }

    private fun setupStreakRecyclerView() {
        Log.d("StreakDebug", "Setting up RecyclerView")

        streakAdapter = StreakAdapter()
        view?.findViewById<RecyclerView>(R.id.rv_streak)?.let { recyclerView ->
            recyclerView.apply {
                adapter = streakAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                visibility = View.VISIBLE

                // Add logging to verify setup
                Log.d("StreakDebug", "RecyclerView setup complete")
            }
        } ?: run {
            Log.e("StreakDebug", "RecyclerView not found!")
        }
    }


    private fun setupMonthNavigation() {
        view?.findViewById<ImageButton>(R.id.btn_previous_month)?.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            updateStreakDates()
        }

        view?.findViewById<ImageButton>(R.id.btn_next_month)?.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            updateStreakDates()
        }
    }

    private fun updateMonthDisplay() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        view?.findViewById<TextView>(R.id.tv_month_year)?.text = monthFormat.format(calendar.time)
    }

    private fun updateStreakDates() {
        if (!isAdded) return  // Early return if fragment is not attached

        Log.d("StreakDebug", "Starting updateStreakDates")
        val dates = mutableListOf<StreakItem>()
        val currentCalendar = calendar.clone() as Calendar
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        Log.d("StreakDebug", "Fetching workouts for ${currentCalendar.get(Calendar.YEAR)}-${currentCalendar.get(Calendar.MONTH) + 1}")

        workoutRepository.getCompletedWorkouts(
            year = currentCalendar.get(Calendar.YEAR),
            month = currentCalendar.get(Calendar.MONTH) + 1,
            onSuccess = onSuccess@{ completedDates ->
                try {
                    if (!isAdded) return@onSuccess  // Check if still attached

                    Log.d("StreakDebug", "Got completed dates: $completedDates")

                    for (i in 1..daysInMonth) {
                        currentCalendar.set(Calendar.DAY_OF_MONTH, i)
                        val dateString = dateFormat.format(currentCalendar.time)

                        val isToday = (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                                currentCalendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))

                        val isCompleted = completedDates.contains(dateString)
                        Log.d("StreakDebug", "Date: $dateString, Completed: $isCompleted")

                        dates.add(StreakItem(
                            date = i.toString(),
                            day = dayFormat.format(currentCalendar.time).uppercase(),
                            isWorkoutDay = isCompleted,
                            isToday = isToday,
                            isCompleted = isCompleted
                        ))
                    }

                    view?.post {
                        if (isAdded && view != null) {  // Double check we're still attached
                            try {
                                streakAdapter.updateStreak(dates)
                            } catch (e: Exception) {
                                Log.e("StreakDebug", "Error updating adapter: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StreakDebug", "Error processing dates: ${e.message}")
                }
            },
            onError = { exception ->
                Log.e("StreakDebug", "Error loading completed workouts: ${exception.message}")
                if (isAdded) {  // Check if attached before falling back
                    updateStreakWithoutCompleted(dates, currentCalendar, today)
                }
            }
        )
    }

    private fun updateStreakWithoutCompleted(
        dates: MutableList<StreakItem>,
        currentCalendar: Calendar,
        today: Calendar
    ) {
        if (!isAdded) return  // Early return if not attached

        try {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            dates.clear()

            for (i in 1..daysInMonth) {
                currentCalendar.set(Calendar.DAY_OF_MONTH, i)

                val isToday = (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        currentCalendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))

                dates.add(StreakItem(
                    date = i.toString(),
                    day = dayFormat.format(currentCalendar.time).uppercase(),
                    isWorkoutDay = false,
                    isToday = isToday,
                    isCompleted = false
                ))
            }

            // Use view?.post instead of requireActivity().runOnUiThread
            view?.post {
                if (isAdded && view != null) {  // Double check we're still attached
                    try {
                        streakAdapter.updateStreak(dates)
                    } catch (e: Exception) {
                        Log.e("StreakDebug", "Error updating adapter: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StreakDebug", "Error in updateStreakWithoutCompleted: ${e.message}")
        }
    }

    private fun updateProgress() {
        if (!isAdded) return
        view?.apply {

            findViewById<TextView>(R.id.tv_calories)?.text = "$currentCalories / $caloriesGoal"
            val caloriesProgress = (currentCalories.toFloat() / caloriesGoal * 100).toInt()

            findViewById<TextView>(R.id.tv_water)?.text = String.format("%.1fL / %.1fL", currentWater, waterGoal)
            val waterProgress = ((currentWater / waterGoal) * 100).toInt()

            findViewById<TextView>(R.id.tv_steps)?.text = "$currentSteps"


            // Steps progress
            val stepsProgress = (currentSteps.toFloat() / stepsGoal.toFloat() * 100).toInt()
            findViewById<CircularProgressIndicator>(R.id.steps_progress)?.progress = stepsProgress
            findViewById<TextView>(R.id.tv_steps)?.text = currentSteps.toString()
            findViewById<TextView>(R.id.tv_steps_target)?.text = "/ $stepsGoal"

            findViewById<CardView>(R.id.card_steps)?.setCardBackgroundColor(
                ContextCompat.getColor(context,
                    if (currentSteps >= stepsGoal) R.color.steps_goal_achieved
                    else R.color.card_background
                )
            )

            findViewById<CardView>(R.id.card_calories)?.setCardBackgroundColor(
                ContextCompat.getColor(context,
                    if (currentCalories >= caloriesGoal) R.color.calories_goal_achieved
                    else R.color.card_background
                )
            )

            findViewById<CardView>(R.id.card_water)?.setCardBackgroundColor(
                ContextCompat.getColor(context,
                    if (currentWater >= waterGoal) R.color.water_goal_achieved
                    else R.color.card_background
                )
            )
        }
    }

    private fun setupProgressListeners() {
        if (!isAdded) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("progress")
            .document(today)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val progress = snapshot.toObject(Progress::class.java)
                    progress?.let {
                        currentCalories = it.calories
                        currentWater = it.water
                        currentSteps = it.steps
                        completedMeals.clear()
                        completedMeals.addAll(it.completedMeals)
                        updateProgress()
                    }
                }
            }
    }

    private fun checkStepSensorAvailability() {
        if (!isAdded) return  // Add this check

        try {
            val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            if (stepSensor == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Step Counter Unavailable")
                    .setMessage("Your device doesn't have a built-in step counter sensor. The step counting feature won't be available.")
                    .setPositiveButton("OK", null)
                    .show()

                view?.findViewById<CardView>(R.id.card_steps)?.apply {
                    alpha = 0.5f
                    isEnabled = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking step sensor: ${e.message}")
        }
    }


    private fun checkStepCounterPermission() {
        if (!isAdded) return  // Add this check

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            }
        }
    }

    private fun showGoalsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_goals, null)

        val stepsEdit = dialogView.findViewById<EditText>(R.id.et_steps_goal)
        val caloriesEdit = dialogView.findViewById<EditText>(R.id.et_calories_goal)
        val waterEdit = dialogView.findViewById<EditText>(R.id.et_water_goal)

        stepsEdit.setText(stepsGoal.toString())
        caloriesEdit.setText(caloriesGoal.toString())
        waterEdit.setText(waterGoal.toString())

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Set Daily Goals")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                try {
                    stepsGoal = stepsEdit.text.toString().toIntOrNull() ?: stepsGoal
                    caloriesGoal = caloriesEdit.text.toString().toIntOrNull() ?: caloriesGoal
                    waterGoal = waterEdit.text.toString().toFloatOrNull() ?: waterGoal

                    saveGoals()
                    updateProgress()
                    dialog.dismiss()
                } catch (e: Exception) {
                    showErrorMessage("Please enter valid numbers")
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveGoals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val goals = hashMapOf(
            "stepsGoal" to stepsGoal,
            "caloriesGoal" to caloriesGoal,
            "waterGoal" to waterGoal
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("settings")
            .document("goals")
            .set(goals)
            .addOnFailureListener { e ->
                showErrorMessage("Could not save goals")
            }
    }

    private fun loadGoals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("settings")
            .document("goals")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    stepsGoal = document.getLong("stepsGoal")?.toInt() ?: 10000
                    caloriesGoal = document.getLong("caloriesGoal")?.toInt() ?: 2200
                    waterGoal = document.getDouble("waterGoal")?.toFloat() ?: 2.5f
                    updateProgress()
                }
            }
            .addOnFailureListener { e ->
                showErrorMessage("Could not load goals")
            }
    }

    private fun setupProgressCards() {
        view?.apply {
            // Calories Card Click
            findViewById<CardView>(R.id.card_calories)?.setOnClickListener {
                showAddCaloriesDialog()
            }

            // Water Card Click
            findViewById<CardView>(R.id.card_water)?.setOnClickListener {
                showAddWaterDialog()
            }
        }
    }

    private fun showAddCaloriesDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_calories, null)
        val caloriesInput = dialogView.findViewById<EditText>(R.id.et_calories)

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Add Calories")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val calories = caloriesInput.text.toString().toIntOrNull()
                if (calories != null && calories > 0) {
                    addCalories(calories)
                    showSuccessMessage("Added $calories calories")
                } else {
                    showErrorMessage("Please enter a valid number")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddWaterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_water, null)
        val waterInput = dialogView.findViewById<EditText>(R.id.et_water)

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Add Water")
            .setView(dialogView)
            .setMessage("Enter amount in ml (250ml = 1 glass)")
            .setPositiveButton("Add") { dialog, _ ->
                val waterMl = waterInput.text.toString().toFloatOrNull()
                if (waterMl != null && waterMl > 0) {
                    addWater(waterMl / 1000f) // Convert to liters
                    showSuccessMessage("Added ${waterMl}ml of water")
                } else {
                    showErrorMessage("Please enter a valid amount")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCalories(calories: Int) {
        currentCalories += calories
        saveCurrentProgress()
        updateProgress()

        // Check if goal is reached and animate
        if (currentCalories >= caloriesGoal) {
            view?.findViewById<CardView>(R.id.card_calories)?.let { card ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.calories_goal_achieved))
                animateCard(card)
            }
        }
    }

    private fun addWater(liters: Float) {
        currentWater += liters
        saveCurrentProgress()
        updateProgress()

        // Check if goal is reached and animate
        if (currentWater >= waterGoal) {
            view?.findViewById<CardView>(R.id.card_water)?.let { card ->
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.water_goal_achieved))
                animateCard(card)
            }
        }
    }

    private fun animateCard(card: CardView) {
        card.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun loadTodayWorkoutAndMeal() {
        val currentDay = getCurrentDayOfWeek()

        // Load workout using WorkoutRepository
        workoutRepository.getWorkoutRoutine(
            onSuccess = { workouts ->
                val todayWorkout = workouts.find { it.date == currentDay }
                updateWorkoutCard(todayWorkout)
            },
            onError = { exception ->
                Log.e("HomeFragment", "Error loading workout: ${exception.message}")
                showErrorMessage("Could not load workout")
            }
        )

        // Load meals using NutritionRepository
        nutritionRepository.getDietPlans(
            onSuccess = { dietPlans ->
                val todayPlan = dietPlans.find { it.date == currentDay }
                currentMeals = todayPlan?.meals
                updateMealCard(todayPlan?.meals)
            },
            onError = { exception ->
                Log.e("HomeFragment", "Error loading meal plan: ${exception.message}")
                showErrorMessage("Could not load meal plan")
            }
        )
    }

    private fun getCurrentDayOfWeek(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> ""
        }
    }

    private fun navigateToWorkout(workout: Workout) {
        if (!isAdded) return

        val workoutFragment = WorkoutExerciseFragment().apply {
            arguments = Bundle().apply {
                putString("day", workout.date)
                putLong("startTime", System.currentTimeMillis())
            }
            setWorkoutCompletionListener(object : WorkoutExerciseFragment.WorkoutCompletionListener {
                override fun onWorkoutCompleted() {
                    if (!isAdded) return

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().time)
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

                    activity?.let { fragmentActivity ->
                        workoutRepository.saveCompletedWorkout(
                            date = today,
                            onSuccess = {
                                // Update workout stats
                                workoutRepository.updateWorkoutStats(
                                    userId = userId,
                                    onSuccess = {
                                        // Update streak
                                        workoutRepository.updateStreak(
                                            userId = userId,
                                            day = today,
                                            onSuccess = { newStreak ->
                                                fragmentActivity.runOnUiThread {
                                                    if (!isAdded) return@runOnUiThread
                                                    try {
                                                        // Update UI components
                                                        updateWorkoutCard(workout)
                                                        updateStreakDates()
                                                        loadTodayWorkoutAndMeal()

                                                        // Refresh the home fragment data
                                                        loadCurrentProgress()
                                                        checkAndResetDaily()
                                                        updateProgress()

                                                        showSuccessMessage("Workout completed! Current streak: $newStreak days")
                                                    } catch (e: Exception) {
                                                        Log.e("WorkoutDebug", "Error updating UI: ${e.message}")
                                                    }
                                                }
                                            },
                                            onError = { exception ->
                                                Log.e("WorkoutDebug", "Failed to update streak: ${exception.message}")
                                            }
                                        )
                                    },
                                    onError = { exception ->
                                        Log.e("WorkoutDebug", "Failed to update workout stats: ${exception.message}")
                                    }
                                )
                            },
                            onError = { exception ->
                                fragmentActivity.runOnUiThread {
                                    if (!isAdded) return@runOnUiThread
                                    Log.e("WorkoutDebug", "Failed to save workout completion: ${exception.message}")
                                    showErrorMessage("Failed to save workout completion")
                                }
                            }
                        )
                    }
                }
            })
        }

        try {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, workoutFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("WorkoutDebug", "Error navigating to workout: ${e.message}")
            showErrorMessage("Failed to start workout")
        }
    }

    private fun updateWorkoutCard(workout: Workout?) {
        // Check if fragment is attached and view is available
        if (!isAdded || view == null) return

        try {
            workoutCardView.apply {
                val workoutContent = findViewById<LinearLayout>(R.id.workout_content)
                val noWorkoutView = findViewById<TextView>(R.id.tv_no_workout)
                val dayView = findViewById<TextView>(R.id.tv_workout_day)
                val nameView = findViewById<TextView>(R.id.tv_workout_name)
                val previewView = findViewById<TextView>(R.id.tv_exercise_preview)
                val fabStartWorkout = findViewById<FloatingActionButton>(R.id.fab_start_workout)

                if (workout != null && workout.exercises.isNotEmpty()) {
                    workoutContent.visibility = View.VISIBLE
                    noWorkoutView.visibility = View.GONE

                    // Format today's date
                    val today = Calendar.getInstance()
                    val displayDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDisplayDate = displayDateFormat.format(today.time)
                    val todayStr = dbDateFormat.format(today.time)

                    Log.d("WorkoutDebug", "Checking completion status for date: $todayStr")

                    // Update UI elements safely
                    dayView.text = formattedDisplayDate
                    nameView.text = "${workout.date}'s Workout"

                    val exerciseCount = workout.exercises.size
                    val estimatedDuration1 = (exerciseCount * 10) - 10
                    val estimatedDuration2 = (exerciseCount * 10) + 10
                    previewView.text = "$exerciseCount exercises • ${estimatedDuration1}-${estimatedDuration2} min"

                    // Check completion status with safe context handling
                    workoutRepository.isWorkoutCompletedForDate(
                        date = todayStr,
                        onSuccess = { isCompleted ->
                            Log.d("WorkoutDebug", "Workout completion status for $todayStr: $isCompleted")

                            // Use Handler for UI updates
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    // Check if fragment is still attached
                                    if (!isAdded) return@post

                                    fabStartWorkout?.apply {
                                        if (isCompleted) {
                                            Log.d("WorkoutDebug", "Setting FAB to completed state")
                                            setImageResource(R.drawable.ic_check)
                                            isEnabled = true
                                            alpha = 0.5f
                                            // Remove click listener for completed workouts
                                            setOnClickListener(null)
                                        } else {
                                            Log.d("WorkoutDebug", "Setting FAB to start state")
                                            setImageResource(R.drawable.ic_play)
                                            isEnabled = true
                                            alpha = 1.0f

                                            // Set click listener safely
                                            setOnClickListener { view ->
                                                if (isAdded) {
                                                    Log.d("WorkoutDebug", "FAB clicked, navigating to workout")
                                                    navigateToWorkout(workout)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("WorkoutDebug", "Error updating FAB: ${e.message}")
                                }
                            }
                        },
                        onError = { exception ->
                            Log.e("WorkoutDebug", "Error checking workout status: ${exception.message}")

                            Handler(Looper.getMainLooper()).post {
                                try {
                                    // Check if fragment is still attached
                                    if (!isAdded) return@post

                                    fabStartWorkout?.apply {
                                        setImageResource(R.drawable.ic_play)
                                        isEnabled = true
                                        alpha = 1.0f

                                        // Set click listener safely
                                        setOnClickListener { view ->
                                            if (isAdded) {
                                                navigateToWorkout(workout)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("WorkoutDebug", "Error handling error state: ${e.message}")
                                }
                            }
                        }
                    )
                } else {
                    Log.d("WorkoutDebug", "No workout available, showing empty state")
                    workoutContent.visibility = View.GONE
                    noWorkoutView.visibility = View.VISIBLE
                    fabStartWorkout.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutDebug", "Error in updateWorkoutCard: ${e.message}")
        }
    }


    private fun updateMealCard(meals: List<Meal>?) {
        Log.d("MealDebug", "Updating meal card with meals: ${meals?.size}")

        val mealCardView = view?.findViewById<CardView>(R.id.card_current_meal) ?: return

        mealCardView.apply {
            val mealContent = findViewById<LinearLayout>(R.id.meal_content)
            val noMealView = findViewById<TextView>(R.id.tv_no_meal)
            val timeView = findViewById<TextView>(R.id.tv_meal_time)
            val nameView = findViewById<TextView>(R.id.tv_meal_name)
            val detailsView = findViewById<TextView>(R.id.tv_meal_details)
            val fabStartMeal = findViewById<FloatingActionButton>(R.id.fab_start_meal)

            if (meals.isNullOrEmpty()) {
                Log.d("MealDebug", "No meals available")
                mealContent.visibility = View.GONE
                noMealView.visibility = View.VISIBLE
                fabStartMeal.visibility = View.GONE
                return@apply
            }

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            Log.d("MealDebug", "Current hour: $currentHour")

            // Map meals to their time slots
            val mealMap = meals.associateBy {
                it.time.lowercase().trim()
            }
            Log.d("MealDebug", "Available meals: ${mealMap.keys}")

            val currentMeal = when {
                currentHour < 10 -> mealMap["breakfast"]
                currentHour < 15 -> mealMap["lunch"]
                currentHour < 19 -> mealMap["snack"] ?: mealMap["evening snack"]
                else -> mealMap["dinner"]
            }
            Log.d("MealDebug", "Selected meal: ${currentMeal?.time}")

            if (currentMeal != null) {
                mealContent.visibility = View.VISIBLE
                noMealView.visibility = View.GONE

                // Set meal time with fixed schedule
                val (displayTime, scheduledTime) = when (currentMeal.time.lowercase().trim()) {
                    "breakfast" -> Pair("Breakfast", "9:00 AM")
                    "lunch" -> Pair("Lunch", "1:00 PM")
                    "snack", "evening snack" -> Pair("Evening Snack", "4:00 PM")
                    "dinner" -> Pair("Dinner", "7:00 PM")
                    else -> Pair(currentMeal.time, "")
                }

                timeView.text = if (scheduledTime.isNotEmpty()) {
                    "$displayTime ($scheduledTime)"
                } else {
                    displayTime
                }

                nameView.text = currentMeal.name

                val details = buildString {
                    append("${currentMeal.calories} calories")
                    if (currentMeal.protein > 0) append(" • ${currentMeal.protein}g protein")
                    if (currentMeal.carbs > 0) append(" • ${currentMeal.carbs}g carbs")
                }
                detailsView.text = details

                // Handle meal completion button
                if (completedMeals.contains(currentMeal.time.lowercase().trim())) {
                    // Meal already completed
                    fabStartMeal.isEnabled = false
                    fabStartMeal.alpha = 0.5f
                    fabStartMeal.setImageResource(R.drawable.ic_check)
                } else {
                    // Meal not completed yet
                    fabStartMeal.isEnabled = true
                    fabStartMeal.alpha = 1.0f
                    fabStartMeal.setImageResource(R.drawable.ic_check)
                    fabStartMeal.setOnClickListener {
                        // Show confirmation dialog
                        MaterialAlertDialogBuilder(context, R.style.AlertDialog_Dark)
                            .setTitle("Complete Meal")
                            .setMessage("Mark ${currentMeal.name} as completed? This will add ${currentMeal.calories} calories to your daily total.")
                            .setPositiveButton("Complete") { dialog, _ ->
                                completeMeal(currentMeal)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
                fabStartMeal.visibility = View.VISIBLE
            } else {
                Log.d("MealDebug", "No meal found for current time")
                mealContent.visibility = View.GONE
                noMealView.visibility = View.VISIBLE
                fabStartMeal.visibility = View.GONE
            }
        }
    }

    private fun completeMeal(meal: Meal) {
        // Add meal to completed set (store in lowercase for consistency)
        completedMeals.add(meal.time.lowercase().trim())

        // Add calories to current total
        currentCalories += meal.calories
        saveCurrentProgress()

        // Update UI
        updateProgress()
        updateMealCard(currentMeals) // Refresh meal card to update button state

        // Show success animation and message
        val view = requireView()
        val caloriesCard = view.findViewById<CardView>(R.id.card_calories)
        caloriesCard?.let {
            it.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }

        showSuccessMessage("${meal.name} completed! Added ${meal.calories} calories.")
    }


    private fun saveCurrentProgress() {
        val progress = Progress(
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: return,
            date = today,
            calories = currentCalories,
            water = currentWater,
            steps = currentSteps,
            completedMeals = completedMeals.toList(),
            stepsGoal = stepsGoal,
            caloriesGoal = caloriesGoal,
            waterGoal = waterGoal,
            timestamp = System.currentTimeMillis()
        )

        progressRepository.saveProgress(
            progress = progress,
            onSuccess = {
                Log.d(TAG, "Progress saved successfully")
            },
            onError = { exception ->
                Log.e("HomeFragment", "Error saving progress: ${exception.message}")
                showErrorMessage("Could not save progress")
            }
        )
    }

    private fun loadCurrentProgress() {
        progressRepository.getProgress(
            date = today,
            onSuccess = { progress ->
                if (progress.userId.isNotEmpty()) {
                    val savedSteps = currentSteps // Store current steps
                    currentCalories = progress.calories
                    currentWater = progress.water
                    currentSteps = maxOf(savedSteps, progress.steps) // Keep higher value
                    stepsGoal = progress.stepsGoal
                    caloriesGoal = progress.caloriesGoal
                    waterGoal = progress.waterGoal
                    completedMeals.clear()
                    completedMeals.addAll(progress.completedMeals)
                    updateProgress()
                }
            },
            onError = { exception ->
                Log.e("HomeFragment", "Error loading progress: ${exception.message}")
                showErrorMessage("Could not load progress")
            }
        )
    }

    private fun checkAndResetDaily() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        // Only reset if it's a new day
        if (currentDate != today) {
            progressRepository.getProgress(
                date = currentDate,
                onSuccess = { progress ->
                    if (progress.userId.isEmpty()) {
                        // Keep steps from sensor but reset other values
                        val currentStepsCount = currentSteps
                        currentCalories = 0
                        currentWater = 0f
                        completedMeals.clear()
                        currentSteps = currentStepsCount
                        saveCurrentProgress()
                    }
                    // Update today reference
                    today = currentDate
                },
                onError = { exception ->
                    Log.e("HomeFragment", "Error checking daily reset: ${exception.message}")
                }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    stepCounter.start()
                }
            }
        }
    }

    private fun handleError(exception: Exception, operation: String) {
        Log.e("HomeFragment", "Error during $operation: ${exception.message}")
        val errorMessage = when (exception) {
            is FirebaseFirestoreException -> "Database error: ${exception.message}"
            is NetworkErrorException -> "Network error. Please check your connection"
            else -> "An error occurred: ${exception.message}"
        }
        showErrorMessage(errorMessage)
    }



    override fun onResume() {
        super.onResume()
        if (!isAdded) return
        loadProfileImage()
        stepCounter.start()
        loadCurrentProgress()
        checkAndResetDaily()
        updateProgress()
        updateStreakDates()
    }

    override fun onPause() {
        super.onPause()
        if (!isAdded) return
        saveCurrentProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isAdded) return
        // Clean up any listeners
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isUpdatingWorkoutCard = false
        // Clean up any references
    }

    override fun onDetach() {
        super.onDetach()
        isUpdatingWorkoutCard = false
    }

    companion object {
        private const val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
    }

    private fun showErrorMessage(message: String) {
        if (!isAdded) return  // Add this check

        try {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing message: ${e.message}")
        }
    }

    private fun showSuccessMessage(message: String) {
        if (!isAdded) return  // Add this check

        try {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing message: ${e.message}")
        }
    }

    private fun setupEditGoalsButton() {
        view?.findViewById<ImageButton>(R.id.btn_edit_goals)?.setOnClickListener {
            showGoalsDialog()
        }
    }
}