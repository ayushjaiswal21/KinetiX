package com.asdevs.kinematix.workoutPlanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.Exercise
import com.asdevs.kinematix.models.Workout

class WorkoutAdapter(
    private val weekDays: List<String>,
    private val onWorkoutClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit, // This will now edit exercises
    private val onDeleteClick: (Int) -> Unit,
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    private var workouts = listOf<Workout>()

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvDate)
        val exercisesList: RecyclerView = view.findViewById(R.id.rvExercises)
        val exerciseCount: TextView = view.findViewById(R.id.tvExerciseCount)
        val editButton: ImageButton = view.findViewById(R.id.btnEdit)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]

        // Set date
        holder.dateText.text = workout.date

        // Set exercise count
        val exerciseCount = workout.exercises.size
        holder.exerciseCount.text = "$exerciseCount exercise${if (exerciseCount != 1) "s" else ""}"

        // Setup exercises list
        holder.exercisesList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ExerciseAdapter(exercises = workout.exercises)
        }

        // Setup click listeners
        holder.itemView.setOnClickListener { onWorkoutClick(position) }
        holder.editButton.setOnClickListener { onEditClick(position) } // Edit exercises
        holder.deleteButton.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount() = workouts.size

    fun updateWorkouts(newWorkouts: List<Workout>) {
        workouts = newWorkouts.sortedBy { workout ->
            weekDays.indexOf(workout.date)
        }
        notifyDataSetChanged()
    }

    fun getWorkouts(): List<Workout> = workouts.toList()
}

class ExerciseAdapter(
    private val exercises: List<Exercise>
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val exerciseName: TextView = view.findViewById(R.id.tvExerciseName)
        val exerciseDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        // Set exercise name
        holder.exerciseName.text = exercise.name

        // Set exercise details
        holder.exerciseDetails.text = buildString {
            append("Sets: ${exercise.sets}")
            append(" | Reps: ${exercise.reps}")
            if (exercise.weight.isNotBlank()) {
                append(" | Weight: ${exercise.weight}")
            }
            if (exercise.rest.isNotBlank()) {
                append(" | Rest: ${exercise.rest}")
            }
        }
    }

    override fun getItemCount() = exercises.size
}