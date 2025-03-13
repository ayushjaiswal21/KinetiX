package com.asdevs.kinematix.models

enum class ExerciseType {
    // Strength Training
    SQUAT,
    BENCH_PRESS,
    DEADLIFT,
    SHOULDER_PRESS,
    BICEP_CURL,
    TRICEP_EXTENSION,

    // Yoga Poses
    WARRIOR_POSE,
    TREE_POSE,
    DOWNWARD_DOG,
    COBRA_POSE,
    CHAIR_POSE,

    // General
    POSTURE,
    PLANK;

    fun getTargetAngles(): Map<String, ClosedRange<Float>> {
        return when (this) {
            // Existing exercises
            SQUAT -> mapOf(
                "knee" to 80f..100f,
                "hip" to 70f..90f,
                "ankle" to 60f..80f,
                "back" to 160f..180f
            )
            BENCH_PRESS -> mapOf(
                "elbow" to 85f..95f,
                "shoulder" to 70f..90f,
                "wrist" to 170f..190f,
                "symmetry" to 0f..15f
            )
            DEADLIFT -> mapOf(
                "hip" to 80f..100f,
                "knee" to 70f..90f,
                "back" to 170f..180f,
                "shoulder" to 170f..190f
            )

            // New strength exercises
            SHOULDER_PRESS -> mapOf(
                "elbow" to 85f..95f,
                "shoulder" to 165f..180f,
                "wrist" to 170f..190f,
                "back" to 170f..190f,
                "symmetry" to 0f..10f
            )
            BICEP_CURL -> mapOf(
                "elbow" to 30f..45f,
                "shoulder" to 170f..190f,
                "wrist" to 170f..190f,
                "symmetry" to 0f..10f
            )
            TRICEP_EXTENSION -> mapOf(
                "elbow" to 150f..170f,
                "shoulder" to 30f..45f,
                "wrist" to 170f..190f,
                "symmetry" to 0f..10f
            )

            // Yoga poses
            WARRIOR_POSE -> mapOf(
                "front_knee" to 85f..95f,
                "back_leg" to 165f..180f,
                "hips" to 85f..95f,
                "torso" to 170f..190f,
                "arms" to 170f..190f
            )
            TREE_POSE -> mapOf(
                "standing_leg" to 170f..190f,
                "knee" to 85f..95f,
                "hip" to 85f..95f,
                "spine" to 170f..190f,
                "arms" to 170f..190f
            )
            DOWNWARD_DOG -> mapOf(
                "shoulder" to 170f..190f,
                "hip" to 60f..80f,
                "knee" to 170f..190f,
                "ankle" to 60f..80f
            )
            COBRA_POSE -> mapOf(
                "elbow" to 85f..95f,
                "shoulder" to 85f..95f,
                "hip" to 170f..190f,
                "spine_extension" to 30f..45f
            )
            CHAIR_POSE -> mapOf(
                "knee" to 85f..95f,
                "hip" to 85f..95f,
                "ankle" to 60f..80f,
                "spine" to 170f..190f,
                "arms" to 170f..190f
            )
            PLANK -> mapOf(
                "elbow" to 85f..95f,
                "shoulder" to 170f..190f,
                "hip" to 170f..190f,
                "knee" to 170f..190f,
                "ankle" to 85f..95f
            )
            POSTURE -> mapOf(
                "neck" to 160f..180f,
                "shoulder" to 80f..100f,
                "spine" to 170f..190f,
                "hip" to 170f..190f,
                "symmetry" to 0f..15f
            )
        }
    }

    fun getKeyCheckpoints(): List<String> {
        return when (this) {
            // Existing checkpoints
            SQUAT -> listOf(
                "Knees tracking over toes",
                "Back straight",
                "Chest up",
                "Proper depth"
            )
            BENCH_PRESS -> listOf(
                "Wrists straight",
                "Elbows at 90°",
                "Even bar path",
                "Shoulders stable"
            )
            DEADLIFT -> listOf(
                "Back straight",
                "Bar close to legs",
                "Shoulders over bar",
                "Hip hinge"
            )

            // New strength exercise checkpoints
            SHOULDER_PRESS -> listOf(
                "Wrists straight",
                "Full extension overhead",
                "Core engaged",
                "Back straight"
            )
            BICEP_CURL -> listOf(
                "Elbows fixed",
                "Controlled movement",
                "Wrists straight",
                "Even movement"
            )
            TRICEP_EXTENSION -> listOf(
                "Elbows close to head",
                "Full extension",
                "Controlled movement",
                "Core engaged"
            )

            // Yoga pose checkpoints
            WARRIOR_POSE -> listOf(
                "Front knee at 90°",
                "Back leg straight",
                "Hips square",
                "Arms extended",
                "Torso upright"
            )
            TREE_POSE -> listOf(
                "Standing leg straight",
                "Foot above knee",
                "Hips level",
                "Spine straight",
                "Arms balanced"
            )
            DOWNWARD_DOG -> listOf(
                "Hands shoulder-width",
                "Heels reaching down",
                "Back straight",
                "Arms straight",
                "Hips lifted"
            )
            COBRA_POSE -> listOf(
                "Shoulders down",
                "Elbows slightly bent",
                "Hips on ground",
                "Chest lifted",
                "Neck neutral"
            )
            CHAIR_POSE -> listOf(
                "Knees over ankles",
                "Thighs parallel to ground",
                "Torso lifted",
                "Arms raised",
                "Weight in heels"
            )
            PLANK -> listOf(
                "Shoulders over elbows",
                "Body straight line",
                "Core engaged",
                "Neck neutral",
                "Hips level"
            )
            POSTURE -> listOf(
                "Head aligned with spine",
                "Shoulders level and relaxed",
                "Spine straight",
                "Hips level",
                "Body weight evenly distributed"
            )
        }
    }

    // Add category grouping
    fun getCategory(): ExerciseCategory {
        return when (this) {
            SQUAT, BENCH_PRESS, DEADLIFT, SHOULDER_PRESS, BICEP_CURL, TRICEP_EXTENSION ->
                ExerciseCategory.STRENGTH
            WARRIOR_POSE, TREE_POSE, DOWNWARD_DOG, COBRA_POSE, CHAIR_POSE ->
                ExerciseCategory.YOGA
            PLANK, POSTURE ->
                ExerciseCategory.GENERAL
        }
    }
}

enum class ExerciseCategory {
    STRENGTH,
    YOGA,
    GENERAL
}