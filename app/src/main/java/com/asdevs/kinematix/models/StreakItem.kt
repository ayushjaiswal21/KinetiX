package com.asdevs.kinematix.models

data class StreakItem(
    val date: String,
    val day: String,
    val isWorkoutDay: Boolean,
    val isToday: Boolean = false,
    val isCompleted: Boolean = false
)