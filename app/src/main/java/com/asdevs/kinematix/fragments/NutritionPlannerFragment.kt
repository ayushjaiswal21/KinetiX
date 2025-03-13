package com.asdevs.kinematix.fragments


import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.UserProfileManager
import com.asdevs.kinematix.database.FirestoreNutritionRepository
import com.asdevs.kinematix.dietPlanner.*
import com.asdevs.kinematix.models.DietPlan
import com.asdevs.kinematix.models.Meal
import com.asdevs.kinematix.models.NutritionProfile
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate

class NutritionPlannerFragment : Fragment() {
    private lateinit var dietPlanAdapter: DietPlanAdapter
    private lateinit var repository: FirestoreNutritionRepository
    private var isDataLoaded = false
    private var currentProfile: NutritionProfile? = null
    private var loadingOverlay: View? = null
    private var saveButton: MaterialButton? = null
    private var isDirty = false
    private val userProfileManager = UserProfileManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nutrition_planner, container, false)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        saveButton = view.findViewById(R.id.saveDietPlanButton)

        repository = FirestoreNutritionRepository()
        setupRecyclerView(view)
        setupButtons(view)
        loadDietPlans()

        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDietPlans)
        dietPlanAdapter = DietPlanAdapter(
            onDietPlanClick = { position -> showDietPlanDetails(position) },
            onEditClick = { position -> editDietPlan(position) },
            onDeleteClick = { position -> deleteDietPlan(position) }
        )
        recyclerView.apply {
            adapter = dietPlanAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialCardView>(R.id.btnCreateDietPlan).setOnClickListener {
            showProfileSetupDialog()
        }
        saveButton = view.findViewById<MaterialButton>(R.id.saveDietPlanButton).apply {
            setOnClickListener {
                saveDietPlans()
            }
            isEnabled = false
        }

        // Update this to pass the position of today's plan or create a new one
        view.findViewById<MaterialCardView>(R.id.btnAddMeal).setOnClickListener {
            val currentPlans = dietPlanAdapter.getDietPlans()
            val today = LocalDate.now().toString()
            val todayPlanIndex = currentPlans.indexOfFirst { it.date == today }

            if (todayPlanIndex != -1) {
                showAddMealDialog(todayPlanIndex)
            } else {
                // If no plan exists for today, create one and add it to the list
                val newPlan = DietPlan(
                    date = today,
                    meals = mutableListOf(),
                    totalCalories = 0,
                    totalProtein = 0,
                    totalCarbs = 0,
                    totalFats = 0
                )
                val updatedPlans = currentPlans.toMutableList()
                updatedPlans.add(newPlan)
                dietPlanAdapter.updateDietPlans(updatedPlans)
                showAddMealDialog(updatedPlans.size - 1)
            }
        }

        saveButton = view.findViewById<MaterialButton>(R.id.saveDietPlanButton).apply {
            setOnClickListener {
                saveDietPlans()
            }
            isEnabled = false
            // Set initial state
            setIconResource(R.drawable.ic_save)
            text = "Save"
        }
    }

    private fun showProfileSetupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_nutrition_profile_setup, null)

        setupSpinners(dialogView)
        loadUserProfile(dialogView)

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Setup Your Nutrition Profile")
            .setView(dialogView)
            .setPositiveButton("Create Diet Plan") { _, _ ->
                val profile = validateProfile(dialogView)
                if (profile != null) {
                    currentProfile = profile
                    generateDietPlan(profile)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        val dietTypeAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.diet_types,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val goalAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.nutrition_goals,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val activityAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.activity_levels,
            R.layout.spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Apply adapters to spinners
        dialogView.apply {
            findViewById<Spinner>(R.id.spinnerGender).adapter = genderAdapter
            findViewById<Spinner>(R.id.spinnerDietType).adapter = dietTypeAdapter
            findViewById<Spinner>(R.id.spinnerGoal).adapter = goalAdapter
            findViewById<Spinner>(R.id.spinnerActivityLevel).adapter = activityAdapter
        }
    }

    private fun markUnsavedChanges() {
        isDirty = true
        saveButton?.apply {
            isEnabled = true
            setIconResource(R.drawable.ic_save)
            text = "Save"
        }
    }

    private fun validateProfile(dialogView: View): NutritionProfile? {
        try {
            val dietTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerDietType)
            val goalSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGoal)
            val activityLevelSpinner = dialogView.findViewById<Spinner>(R.id.spinnerActivityLevel)

            val dietType = dietTypeSpinner.selectedItem.toString()
            val goal = goalSpinner.selectedItem.toString()
            val activityLevel = activityLevelSpinner.selectedItem.toString()

            if (dietType == "Select Diet Type" || goal == "Select Goal" ||
                activityLevel == "Select Activity Level") {
                showError("Please select all options")
                return null
            }

            val mealsPerDay = dialogView.findViewById<TextInputEditText>(R.id.etMealsPerDay)
                ?.text?.toString()?.toIntOrNull() ?: 3

            val allergiesText = dialogView.findViewById<TextInputEditText>(R.id.etAllergies)
                ?.text?.toString() ?: ""
            val allergies = allergiesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Get pre-filled profile data
            val height = dialogView.findViewById<TextInputEditText>(R.id.etHeight)
                .text.toString().toFloatOrNull() ?: 0f
            val weight = dialogView.findViewById<TextInputEditText>(R.id.etWeight)
                .text.toString().toFloatOrNull() ?: 0f
            val age = dialogView.findViewById<TextInputEditText>(R.id.etAge)
                .text.toString().toIntOrNull() ?: 0
            val gender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
                .selectedItem.toString()

            return NutritionProfile(
                dietType = dietType,
                goal = goal,
                activityLevel = activityLevel,
                mealsPerDay = mealsPerDay,
                allergies = allergies,
                height = height,
                weight = weight,
                age = age,
                gender = gender
            )
        } catch (e: Exception) {
            showError("Error creating profile: ${e.message}")
            return null
        }
    }

    private fun generateDietPlan(profile: NutritionProfile) {
        showLoading(true)
        try {
            val mealRecommender = MealRecommender()
            val dietPlans = mealRecommender.recommendMeals(profile)
            dietPlanAdapter.updateDietPlans(dietPlans)
            markUnsavedChanges()
            showSuccess("Diet plan generated successfully! Don't forget to save it.")
        } catch (e: Exception) {
            showError("Failed to generate diet plan: ${e.message}")
        } finally {
            showLoading(false)
        }
    }

    private fun showDietPlanDetails(position: Int) {
        val dietPlan = dietPlanAdapter.getDietPlans()[position]
        val message = buildString {
            appendLine("Date: ${dietPlan.date}")
            appendLine("Total Calories: ${dietPlan.totalCalories}")
            appendLine("Macros:")
            appendLine("- Protein: ${dietPlan.totalProtein}g")
            appendLine("- Carbs: ${dietPlan.totalCarbs}g")
            appendLine("- Fats: ${dietPlan.totalFats}g")
            appendLine("\nMeals:")
            dietPlan.meals.forEach { meal ->
                appendLine("\n${meal.name}")
                appendLine("Time: ${meal.time}")
                appendLine("Calories: ${meal.calories}")
                appendLine("Protein: ${meal.protein}g")
                appendLine("Carbs: ${meal.carbs}g")
                appendLine("Fats: ${meal.fats}g")
                appendLine("\nIngredients:")
                meal.ingredients.forEach { appendLine("- $it") }
                appendLine("\nInstructions:")
                appendLine(meal.instructions)
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Diet Plan Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun editDietPlan(position: Int) {
        val dietPlan = dietPlanAdapter.getDietPlans()[position]
        showEditMealsDialog(position, dietPlan.date)
    }

    private fun showEditMealsDialog(position: Int, date: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Edit Meals")
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Add Meal") { dialog, _ ->
                showAddMealDialog(position)
                dialog.dismiss()
            }
            .show()
    }

    private fun showAddMealDialog(planPosition: Int) {
        try {
            if (!isAdded) return

            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_meal, null)

            // Setup meal type dropdown
            val mealTypeDropdown = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerMealType)
            val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                mealTypes
            )
            mealTypeDropdown.setAdapter(adapter)
            mealTypeDropdown.setText(mealTypes[0], false)

            MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
                .setTitle("Add Meal")
                .setView(dialogView)
                .setPositiveButton("Add") { dialog, _ ->
                    try {
                        val newMeal = Meal(
                            name = dialogView.findViewById<TextInputEditText>(R.id.etMealName)
                                ?.text?.toString().orEmpty(),
                            time = dialogView.findViewById<TextInputEditText>(R.id.etMealTime)
                                ?.text?.toString().orEmpty(),
                            calories = dialogView.findViewById<TextInputEditText>(R.id.etCalories)
                                ?.text?.toString()?.toIntOrNull() ?: 0,
                            protein = dialogView.findViewById<TextInputEditText>(R.id.etProtein)
                                ?.text?.toString()?.toIntOrNull() ?: 0,
                            carbs = dialogView.findViewById<TextInputEditText>(R.id.etCarbs)
                                ?.text?.toString()?.toIntOrNull() ?: 0,
                            fats = dialogView.findViewById<TextInputEditText>(R.id.etFats)
                                ?.text?.toString()?.toIntOrNull() ?: 0,
                            ingredients = dialogView.findViewById<TextInputEditText>(R.id.etIngredients)
                                ?.text?.toString().orEmpty()
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() },
                            instructions = dialogView.findViewById<TextInputEditText>(R.id.etInstructions)
                                ?.text?.toString().orEmpty(),
                            type = mealTypeDropdown.text.toString()
                        )

                        if (newMeal.name.isBlank() || newMeal.type.isBlank()) {
                            showError("Please fill in at least the meal name and type")
                            return@setPositiveButton
                        }

                        // Get current diet plans and update the specific day
                        val currentPlans = dietPlanAdapter.getDietPlans().toMutableList()
                        val currentPlan = currentPlans[planPosition]
                        val currentMeals = currentPlan.meals.toMutableList()

                        // Replace meal of same type or add new one
                        val existingMealIndex = currentMeals.indexOfFirst { it.type == newMeal.type }
                        if (existingMealIndex != -1) {
                            currentMeals[existingMealIndex] = newMeal
                        } else {
                            currentMeals.add(newMeal)
                        }

                        // Update the plan with new meals
                        currentPlans[planPosition] = currentPlan.copy(
                            meals = currentMeals,
                            totalCalories = currentMeals.sumOf { it.calories },
                            totalProtein = currentMeals.sumOf { it.protein },
                            totalCarbs = currentMeals.sumOf { it.carbs },
                            totalFats = currentMeals.sumOf { it.fats }
                        )

                        dietPlanAdapter.updateDietPlans(currentPlans)
                        showSuccess("Meal updated successfully")
                        dialog.dismiss()

                    } catch (e: Exception) {
                        Log.e("NutritionPlanner", "Error adding meal: ${e.message}")
                        showError("Failed to add meal: ${e.message}")
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        } catch (e: Exception) {
            Log.e("NutritionPlanner", "Error showing add meal dialog: ${e.message}")
            showError("Failed to show add meal dialog: ${e.message}")
        }
    }

    private fun deleteDietPlan(position: Int) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Delete Diet Plan")
            .setMessage("Are you sure you want to delete this diet plan?")
            .setPositiveButton("Delete") { _, _ ->
                val currentPlans = dietPlanAdapter.getDietPlans().toMutableList()
                currentPlans.removeAt(position)
                dietPlanAdapter.updateDietPlans(currentPlans)
                showSuccess("Diet plan deleted successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDietPlans() {
        showLoading(true)
        repository.getDietPlans(
            onSuccess = { plans ->
                showLoading(false)
                isDataLoaded = true
                dietPlanAdapter.updateDietPlans(plans)
                markUnsavedChanges()
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to load diet plans: ${e.message}")
            }
        )
    }

    private fun saveDietPlans() {
        val plans = dietPlanAdapter.getDietPlans()
        if (plans.isEmpty()) {
            showError("No diet plans to save")
            return
        }
        showLoading(true)

        repository.saveDietPlan(
            dietPlans = plans,
            onSuccess = {
                showLoading(false)
                showSuccess("Diet plans saved successfully")
                isDirty = false
                saveButton?.apply {
                    isEnabled = false
                    setIconResource(R.drawable.ic_check)
                    text = "Saved"
                }
            },
            onError = { e ->
                showLoading(false)
                showError("Failed to save diet plans: ${e.message}")
            }
        )
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay?.visibility = if (show) View.VISIBLE else View.GONE
        view?.apply {
            findViewById<MaterialCardView>(R.id.btnCreateDietPlan)?.isClickable = !show
            findViewById<MaterialCardView>(R.id.btnAddMeal)?.isClickable = !show
            findViewById<MaterialButton>(R.id.saveDietPlanButton)?.isEnabled = !show
            findViewById<RecyclerView>(R.id.rvDietPlans)?.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}