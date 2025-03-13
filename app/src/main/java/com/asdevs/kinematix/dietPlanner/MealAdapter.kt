package com.asdevs.kinematix.dietPlanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.Meal
import com.google.android.material.button.MaterialButton

class MealAdapter(
    private val meals: List<Meal>,
    private val onMealClick: (Meal) -> Unit
) : RecyclerView.Adapter<MealAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMealTime: TextView = view.findViewById(R.id.tvMealTime)
        val tvMealName: TextView = view.findViewById(R.id.tvMealName)
        val tvCalories: TextView = view.findViewById(R.id.tvCalories)
        val tvMacros: TextView = view.findViewById(R.id.tvMacros)
        val btnShowDetails: MaterialButton = view.findViewById(R.id.btnShowDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = meals[position]

        holder.apply {
            tvMealTime.text = meal.time
            tvMealName.text = meal.name
            tvCalories.text = "${meal.calories} calories"
            tvMacros.text = meal.getMacrosText()

            btnShowDetails.setOnClickListener { onMealClick(meal) }
        }
    }

    override fun getItemCount() = meals.size
}