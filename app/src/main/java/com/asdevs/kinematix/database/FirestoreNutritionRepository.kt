package com.asdevs.kinematix.database

import android.util.Log
import com.asdevs.kinematix.models.DietPlan
import com.asdevs.kinematix.models.Meal
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class FirestoreNutritionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    fun saveDietPlan(
        dietPlans: List<DietPlan>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userDietRef = db.collection("users")
            .document(userId)
            .collection("diet_plans")

        val batch = db.batch()

        userDietRef.get()
            .addOnSuccessListener { snapshot ->
                // Delete existing diet plans
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Add new diet plans with standardized meal times
                dietPlans.forEach { dietPlan ->
                    val newDietPlanRef = userDietRef.document()
                    val dietPlanData = hashMapOf(
                        "date" to dietPlan.date,
                        "totalCalories" to dietPlan.totalCalories,
                        "totalProtein" to dietPlan.totalProtein,
                        "totalCarbs" to dietPlan.totalCarbs,
                        "totalFats" to dietPlan.totalFats,
                        "meals" to dietPlan.meals.map { meal ->
                            // Standardize meal time before saving
                            val standardizedTime = when (meal.time.lowercase().trim()) {
                                "breakfast", "morning" -> "Breakfast"
                                "lunch", "afternoon" -> "Lunch"
                                "snack", "evening snack", "evening" -> "Snack"
                                "dinner", "night" -> "Dinner"
                                else -> "Snack" // Default case
                            }

                            hashMapOf(
                                "name" to meal.name,
                                "time" to standardizedTime, // Use standardized time
                                "calories" to meal.calories,
                                "protein" to meal.protein,
                                "carbs" to meal.carbs,
                                "fats" to meal.fats,
                                "ingredients" to meal.ingredients,
                                "instructions" to meal.instructions
                            )
                        }
                    )
                    batch.set(newDietPlanRef, dietPlanData)
                }

                // Commit all changes
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("DietPlanDebug", "Diet plans saved successfully")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("DietPlanDebug", "Error saving diet plans: ${e.message}")
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DietPlanDebug", "Error accessing diet plans: ${e.message}")
                onError(e)
            }
    }

    // Helper function to standardize meal times (can be used elsewhere)
    private fun standardizeMealTime(time: String): String {
        return when (time.lowercase().trim()) {
            "breakfast", "morning" -> "Breakfast"
            "lunch", "afternoon" -> "Lunch"
            "snack", "evening snack", "evening" -> "Snack"
            "dinner", "night" -> "Dinner"
            else -> "Snack"
        }
    }
    fun getDietPlans(
        onSuccess: (List<DietPlan>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("diet_plans")
            .get()
            .addOnSuccessListener { snapshot ->
                val dietPlans = snapshot.documents.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val mealsData = doc.get("meals") as? List<Map<String, Any>> ?: emptyList()
                        val meals = mealsData.mapNotNull { mealData ->
                            try {
                                Meal(
                                    name = mealData["name"] as? String ?: "",
                                    time = mealData["time"] as? String ?: "",
                                    calories = (mealData["calories"] as? Long)?.toInt() ?: 0,
                                    protein = (mealData["protein"] as? Long)?.toInt() ?: 0,
                                    carbs = (mealData["carbs"] as? Long)?.toInt() ?: 0,
                                    fats = (mealData["fats"] as? Long)?.toInt() ?: 0,
                                    ingredients = (mealData["ingredients"] as? List<String>) ?: emptyList(),
                                    instructions = mealData["instructions"] as? String ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        DietPlan(
                            date = doc.getString("date") ?: "",
                            meals = meals,
                            totalCalories = doc.getLong("totalCalories")?.toInt() ?: 0,
                            totalProtein = doc.getLong("totalProtein")?.toInt() ?: 0,
                            totalCarbs = doc.getLong("totalCarbs")?.toInt() ?: 0,
                            totalFats = doc.getLong("totalFats")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                val sortedDietPlans = dietPlans.sortedBy { plan ->
                    when (plan.date) {
                        "Monday" -> 1
                        "Tuesday" -> 2
                        "Wednesday" -> 3
                        "Thursday" -> 4
                        "Friday" -> 5
                        "Saturday" -> 6
                        "Sunday" -> 7
                        else -> 8
                    }
                }
                onSuccess(sortedDietPlans)
            }
            .addOnFailureListener { onError(it) }
    }
}