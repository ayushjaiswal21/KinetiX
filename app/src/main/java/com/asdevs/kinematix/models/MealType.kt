package com.asdevs.kinematix.models

enum class MealType {
    BREAKFAST,
    MORNING_SNACK,
    LUNCH,
    EVENING_SNACK,
    DINNER;

    fun displayTime(): String = when (this) {
        BREAKFAST -> "8:00 AM"
        MORNING_SNACK -> "10:30 AM"
        LUNCH -> "1:00 PM"
        EVENING_SNACK -> "4:30 PM"
        DINNER -> "8:00 PM"
    }

    fun displayName(): String = when (this) {
        BREAKFAST -> "Breakfast"
        MORNING_SNACK -> "Morning Snack"
        LUNCH -> "Lunch"
        EVENING_SNACK -> "Evening Snack"
        DINNER -> "Dinner"
    }
}