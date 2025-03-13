package com.asdevs.kinematix.dietPlanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.DietPlan
import com.asdevs.kinematix.models.Meal
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DietPlanAdapter(
    private val onDietPlanClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<DietPlanAdapter.ViewHolder>() {

    private var dietPlans: List<DietPlan> = emptyList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvDailyMacros: TextView = view.findViewById(R.id.tvDailyMacros)
        val rvMeals: RecyclerView = view.findViewById(R.id.rvMeals)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dietPlan = dietPlans[position]

        holder.apply {
            tvDate.text = dietPlan.date
            tvDailyMacros.text = "Total: ${dietPlan.totalCalories} cal • P: ${dietPlan.totalProtein}g • C: ${dietPlan.totalCarbs}g • F: ${dietPlan.totalFats}g"

            // Setup MealAdapter for this diet plan
            val mealAdapter = MealAdapter(dietPlan.meals) { meal ->
                showMealDetailsDialog(itemView.context, meal)
            }

            rvMeals.apply {
                adapter = mealAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }

            btnEdit.setOnClickListener { onEditClick(position) }
            btnDelete.setOnClickListener { onDeleteClick(position) }
            itemView.setOnClickListener { onDietPlanClick(position) }
        }
    }

    private fun showMealDetailsDialog(context: Context, meal: Meal) {
        MaterialAlertDialogBuilder(context)
            .setTitle(meal.name)
            .setMessage(buildString {
                appendLine("Time: ${meal.time}")
                appendLine("Calories: ${meal.calories}")
                appendLine("\nMacros:")
                appendLine("Protein: ${meal.protein}g")
                appendLine("Carbs: ${meal.carbs}g")
                appendLine("Fats: ${meal.fats}g")
                appendLine("\nIngredients:")
                meal.ingredients.forEach { appendLine("• $it") }
                appendLine("\nInstructions:")
                appendLine(meal.instructions)
            })
            .setPositiveButton("Close", null)
            .show()
    }

    override fun getItemCount() = dietPlans.size

    fun getDietPlans(): List<DietPlan> = dietPlans

    fun updateDietPlans(newDietPlans: List<DietPlan>) {
        dietPlans = newDietPlans
        notifyDataSetChanged()
    }
}