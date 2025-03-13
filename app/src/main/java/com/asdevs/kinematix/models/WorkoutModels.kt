package com.asdevs.kinematix.models

data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: String = "",
    val rest: String = "",
    val weight: String = "",
    var isCompleted: Boolean = false
)

data class Workout(
    val date: String = "",
    val exercises: List<Exercise> = emptyList()
)
