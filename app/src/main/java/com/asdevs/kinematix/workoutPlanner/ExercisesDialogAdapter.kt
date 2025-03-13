package com.asdevs.kinematix.workoutPlanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.Exercise

class ExercisesDialogAdapter(
    context: Context,
    private val exercises: List<Exercise>,
    private val onExerciseClick: (Int) -> Unit
) : ArrayAdapter<Exercise>(context, R.layout.item_dialog_exercise, exercises) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dialog_exercise, parent, false)

        val exercise = exercises[position]

        view.findViewById<TextView>(R.id.tvExerciseName).text = exercise.name
        view.findViewById<TextView>(R.id.tvExerciseDetails).text = buildString {
            append("${exercise.sets} sets × ${exercise.reps}")
            if (exercise.weight.isNotBlank()) append(" | ${exercise.weight}")
            if (exercise.rest.isNotBlank()) append(" | Rest: ${exercise.rest}")
        }

        view.setOnClickListener { onExerciseClick(position) }

        return view
    }
}