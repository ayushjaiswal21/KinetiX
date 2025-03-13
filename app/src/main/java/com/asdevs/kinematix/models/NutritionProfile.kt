package com.asdevs.kinematix.models

data class NutritionProfile(
    val height: Float,
    val weight: Float,
    val age: Int,
    val gender: String,
    val dietType: String,
    val goal: String,
    val activityLevel: String,
    val mealsPerDay: Int,
    val allergies: List<String> = emptyList(),
    val preferences: List<String> = emptyList()
)

data class Meal(
    val name: String,
    val time: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val ingredients: List<String>,
    val instructions: String,
    val imageUrl: String = "",  // Optional meal image
    val type: String = "Meal"
){
    fun getMacrosText(): String =
        "Protein: ${protein}g • Carbs: ${carbs}g • Fats: ${fats}g"
}

data class DietPlan(
    val date: String,
    val meals: List<Meal>,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFats: Int
)