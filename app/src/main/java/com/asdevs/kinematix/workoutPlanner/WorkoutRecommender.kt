package com.asdevs.kinematix.workoutPlanner

import com.asdevs.kinematix.models.Exercise
import com.asdevs.kinematix.models.Workout


class WorkoutRecommender {
    private val weekDays = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )

    fun recommendWorkout(profile: UserProfile): List<Workout> {
        val selectedDays = when (profile.workoutDaysPerWeek) {
            3 -> listOf("Monday", "Wednesday", "Friday")
            4 -> listOf("Monday", "Tuesday", "Thursday", "Friday")
            5 -> listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
            6 -> listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            7 -> weekDays
            else -> weekDays.take(profile.workoutDaysPerWeek)
        }

        return when (profile.workoutLocation) {
            "Home" -> recommendHomeWorkout(profile, selectedDays)
            "Gym" -> recommendGymWorkout(profile, selectedDays)
            "Yoga" -> recommendYogaWorkout(profile, selectedDays)
            else -> recommendHomeWorkout(profile, selectedDays)
        }
    }

    private fun recommendHomeWorkout(profile: UserProfile, days: List<String>): List<Workout> {
        return when (profile.goal) {
            "Weight Loss" -> generateHomeWeightLossWorkouts(profile, days)
            "Muscle Gain" -> generateHomeStrengthWorkouts(profile, days)
            else -> generateHomeGeneralFitnessWorkouts(profile, days)
        }
    }

    private fun recommendGymWorkout(profile: UserProfile, days: List<String>): List<Workout> {
        return when (profile.goal) {
            "Weight Loss" -> generateGymWeightLossWorkouts(profile, days)
            "Muscle Gain" -> generateGymStrengthWorkouts(profile, days)
            else -> generateGymGeneralFitnessWorkouts(profile, days)
        }
    }

    private fun recommendYogaWorkout(profile: UserProfile, days: List<String>): List<Workout> {
        return generateYogaWorkouts(profile, days)
    }

    private fun generateHomeWeightLossWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val bodyweightCardio = listOf(
            Exercise("Jumping Jacks", 3, "50 reps", rest = "30s"),
            Exercise("Mountain Climbers", 3, "30 reps", rest = "30s"),
            Exercise("High Knees", 3, "30 seconds", rest = "30s"),
            Exercise("Burpees", 3, "12 reps", rest = "45s"),
            Exercise("Squat Jumps", 3, "12 reps", rest = "45s"),
            Exercise("Jump Rope", 3, "30 reps", rest = "30s"),
            Exercise("Frog Jumps", 3, "12 reps", rest = "45s")
        )

        val bodyweightStrength = listOf(
            Exercise("Push-ups", 3, "10-15 reps", rest = "60s"),
            Exercise("Squats", 3, "20 reps", rest = "60s"),
            Exercise("Lunges", 3, "12 each leg", rest = "45s"),
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Glute Bridges", 3, "12 reps", rest = "45s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s")
        )

        days.forEach { day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = bodyweightCardio.shuffled().take(4) + bodyweightStrength.shuffled().take(4)
                )
            )
        }

        return workouts
    }

    private fun generateHomeStrengthWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val upperBody = listOf(
            Exercise("Push-ups", 4, "Max reps", rest = "90s"),
            Exercise("Diamond Push-ups", 3, "Max reps", rest = "90s"),
            Exercise("Pike Push-ups", 3, "Max reps", rest = "90s"),
            Exercise("Dips (using chairs)", 3, "Max reps", rest = "90s"),
            Exercise("Reverse Plank Hold", 3, "30 seconds", rest = "60s")
        )

        val lowerBody = listOf(
            Exercise("Squats", 4, "20 reps", rest = "90s"),
            Exercise("Lunges", 4, "15 each leg", rest = "90s"),
            Exercise("Glute Bridges", 3, "20 reps", rest = "60s"),
            Exercise("Calf Raises", 3, "30 reps", rest = "60s")
        )

        val core = listOf(
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s"),
            Exercise("Russian Twists", 3, "12 reps", rest = "45s")
        )

        days.forEachIndexed { index, day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = if (index % 2 == 0)
                        upperBody.shuffled().take(3) + core.shuffled().take(2)
                    else
                        lowerBody.shuffled().take(3) + core.shuffled().take(2)
                )
            )
        }

        return workouts
    }

    private fun generateHomeGeneralFitnessWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val exercises = listOf(
            Exercise("Push-ups", 3, "10 reps", rest = "60s"),
            Exercise("Squats", 3, "15 reps", rest = "60s"),
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Jumping Jacks", 3, "30 reps", rest = "30s"),
            Exercise("Mountain Climbers", 3, "20 each side", rest = "45s"),
            Exercise("Dips (using chairs)", 3, "Max reps", rest = "90s"),
            Exercise("Lunges", 4, "15 each leg", rest = "90s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s")
        )

        days.forEach { day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = exercises.shuffled().take(6)
                )
            )
        }

        return workouts
    }

    private fun generateGymWeightLossWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val cardio = listOf(
            Exercise("Treadmill(Incline walk)", 1, "20 mins", rest = "60s"),
            Exercise("Elliptical", 1, "15 mins", rest = "60s"),
            Exercise("Rowing Machine", 1, "10 mins", rest = "60s"),
            Exercise("Jump Rope", 2, "3 mins", rest = "60s")
        )

        val upperBody = listOf(
            Exercise("Incline Dumbell Press", 3, "12 reps", weight = "Moderate", rest = "45s"),
            Exercise("Dumbbell Shoulder Press", 3, "10 reps", weight = "Moderate", rest = "60s"),
            Exercise("Lat Pulldown", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Tricep Dips", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Cable Rows", 3, "12-15 reps", weight = "Moderate", rest = "60s")
        )

        val lowerBody = listOf(
            Exercise("Squats", 3, "12 reps", weight = "Moderate", rest = "45s"),
            Exercise("Deadlifts", 3, "10 reps", weight = "Heavy", rest = "60s"),
            Exercise("Walking Lunges", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Calf Raises", 3, "12-15 reps", weight = "Moderate", rest = "60s")
        )

        val core = listOf(
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s"),
            Exercise("Russian Twists", 3, "12 reps", rest = "45s")
        )

        days.forEachIndexed { index, day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = if (index % 2 == 0)
                        cardio.shuffled().take(3) + upperBody.shuffled().take(3)
                    else
                        lowerBody.shuffled().take(4) + core.shuffled().take(3)
                )
            )
        }

        return workouts
    }

    private fun generateGymStrengthWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val chest = listOf(
            Exercise("Flat Bench Press", 3, "8-10 reps", weight = "Heavy", rest = "120s"),
            Exercise("Inclined Dumbbell Press", 3, "10-12 reps", weight = "Heavy", rest = "120s"),
            Exercise("Cable Cross", 3, "10-12 reps", weight = "Light", rest = "60s")
        )

        val shoulder = listOf(
            Exercise("Shoulder Dumbbell Press", 3, "12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Cable Side Raise", 3, "10-12 reps", weight = "Light", rest = "60s"),
            Exercise("Face Pull", 3, "8-10 reps", weight = "Moderate", rest = "90s")
        )

        val triceps = listOf(
            Exercise("Pulley Push Down", 3, "10-12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Overhead Tricep Extension", 3, "10-12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Close Grip Bench", 3, "10-12 reps", weight = "Moderate", rest = "90s")
        )

        val back = listOf(
            Exercise("Lat Pulldown", 3, "10-12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Seated Rowing", 3, "10-12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Cable Pulldown", 3, "10-12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Barbell Rows", 4, "10-12 reps", weight = "Heavy", rest = "120s")
        )

        val biceps = listOf(
            Exercise("Dumbbell Curls", 3, "12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Hammer Curls", 3, "12 reps", weight = "Moderate", rest = "90s"),
            Exercise("Preacher Curls", 3, "12 reps", weight = "Moderate", rest = "90s")
        )

        val leg = listOf(
            Exercise("Squats", 4, "10-12 reps", weight = "Heavy", rest = "120s"),
            Exercise("Leg Press", 4, "8-10 reps", weight = "Heavy", rest = "120s"),
            Exercise("Leg Curl", 4, "8-10 reps", weight = "Heavy", rest = "120s"),
            Exercise("Calf Raises", 3, "15 reps", weight = "Moderate", rest = "90s")
        )

        val core = listOf(
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s"),
            Exercise("Russian Twists", 3, "12 reps", rest = "45s")
        )

        days.forEachIndexed { index, day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = when (index % 3) {
                        0 -> chest.shuffled().take(3) + triceps.shuffled().take(2) + shoulder.shuffled().take(2)
                        1 -> back.shuffled().take(3) + biceps.shuffled().take(3)
                        else -> leg.shuffled().take(3) + core.shuffled().take(2)
                    }
                )
            )
        }

        return workouts
    }

    private fun generateGymGeneralFitnessWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val upperBody = listOf(
            Exercise("Incline Dumbell Press", 3, "12 reps", weight = "Moderate", rest = "45s"),
            Exercise("Dumbbell Shoulder Press", 3, "10 reps", weight = "Moderate", rest = "60s"),
            Exercise("Lat Pulldown", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Tricep Dips", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Cable Rows", 3, "12-15 reps", weight = "Moderate", rest = "60s")
        )

        val lowerBody = listOf(
            Exercise("Squats", 3, "12 reps", weight = "Moderate", rest = "45s"),
            Exercise("Deadlifts", 3, "10 reps", weight = "Heavy", rest = "60s"),
            Exercise("Walking Lunges", 3, "12-15 reps", weight = "Moderate", rest = "60s"),
            Exercise("Calf Raises", 3, "12-15 reps", weight = "Moderate", rest = "60s")
        )

        val core = listOf(
            Exercise("Plank", 3, "30 seconds", rest = "45s"),
            Exercise("Leg Raises", 3, "12 reps", rest = "45s"),
            Exercise("Russian Twists", 3, "12 reps", rest = "45s")
        )

        days.forEachIndexed { index, day ->
            workouts.add(
                Workout(
                    date = day,
                    exercises = if (index % 2 == 0)
                        upperBody.shuffled().take(4) + core.shuffled().take(3)
                    else
                        lowerBody.shuffled().take(4) + core.shuffled().take(3)
                )
            )
        }

        return workouts
    }

    private fun generateYogaWorkouts(profile: UserProfile, days: List<String>): List<Workout> {
        val workouts = mutableListOf<Workout>()

        val beginnerPoses = listOf(
            Exercise("Sun Salutation (सूर्य नमस्कार)", 3, "5 rounds", rest = "30s"),
            Exercise("Cat-Cow Stretch (मार्जरीआसन-बिटिलासन)", 1, "10 rounds", rest = "0s"),
            Exercise("Downward Dog (अधोमुख श्वानासन)", 1, "30 seconds", rest = "30s"),
            Exercise("Warrior I (वीरभद्रासन)", 1, "30 seconds each side", rest = "30s"),
            Exercise("Tree Pose (वृक्षासन)", 1, "30 seconds each side", rest = "30s"),
            Exercise("Child's Pose (बालासन)", 1, "1 minute", rest = "0s")
        )

        val intermediatePoses = listOf(
            Exercise("Sun Salutation B (सूर्य नमस्कार)", 3, "5 rounds", rest = "30s"),
            Exercise("Warrior II (वीरभद्रासन)", 1, "45 seconds each side", rest = "30s"),
            Exercise("Triangle Pose (त्रिकोणासन)", 1, "30 seconds each side", rest = "30s"),
            Exercise("Crow Pose (बकासन)", 3, "15 seconds", rest = "60s"),
            Exercise("Bridge Pose (सेतुबंधासन)", 3, "30 seconds", rest = "30s")
        )

        val shuffledBeginnerPoses = beginnerPoses.shuffled()
        val beginnerPosePairs = shuffledBeginnerPoses.chunked(2)

        days.forEachIndexed { index, day ->
            val dailyPoses = when (profile.fitnessLevel) {
                "Beginner" -> beginnerPoses.shuffled()
                else -> {
                    val pairIndex = index % beginnerPosePairs.size
                    val todayBeginnerPoses = beginnerPosePairs[pairIndex]
                    val randomIntermediatePoses = intermediatePoses.shuffled()
                    todayBeginnerPoses + randomIntermediatePoses
                }
            }

            workouts.add(
                Workout(
                    date = day,
                    exercises = dailyPoses
                )
            )
        }

        return workouts
    }
}