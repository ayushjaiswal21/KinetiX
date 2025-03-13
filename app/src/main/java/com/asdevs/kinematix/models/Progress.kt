package com.asdevs.kinematix.models

data class Progress(
    val userId: String = "",
    val date: String = "",
    val calories: Int = 0,
    val water: Float = 0f,
    val steps: Int = 0,
    val completedMeals: List<String> = listOf(),
    val stepsGoal: Int = 10000,
    val caloriesGoal: Int = 2200,
    val waterGoal: Float = 2.5f,
    val timestamp: Long = System.currentTimeMillis()
)