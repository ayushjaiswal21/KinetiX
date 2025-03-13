package com.asdevs.kinematix.workoutPlanner

data class UserProfile(
    val height: Double = 0.0,  // in cm
    val weight: Double = 0.0,  // in kg
    val age: Int = 0,
    val gender: String = "",
    val fitnessLevel: String = "", // "Beginner", "Intermediate", "Advanced"
    val goal: String = "",  // "Weight Loss", "Muscle Gain", "General Fitness"
    val workoutDaysPerWeek: Int = 0,
    val workoutLocation: String = "" // "Home", "Gym", "Yoga"
)