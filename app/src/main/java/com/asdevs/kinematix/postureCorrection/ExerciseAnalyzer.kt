package com.asdevs.kinematix.postureCorrection

import com.asdevs.kinematix.models.ExerciseType
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

class ExerciseAnalyzer(private val exerciseType: ExerciseType) {
    data class ExerciseFeedback(
        val isCorrectForm: Boolean,
        val message: String,
        val confidence: Float,
        val repCount: Int = 0
    )

    private val repCounter = RepCounter(exerciseType)
    private val targetAngles = exerciseType.getTargetAngles()
    private var lastFeedbackTime = 0L
    private var lastFeedback: ExerciseFeedback? = null
    private val FEEDBACK_DELAY_MS = 1000L // Show feedback for at least 1 second

    fun analyzePose(pose: Pose): ExerciseFeedback {
        val currentTime = System.currentTimeMillis()
        val currentReps = repCounter.getRepCount()

        // Return cached feedback if within delay window
        lastFeedback?.let {
            if (currentTime - lastFeedbackTime < FEEDBACK_DELAY_MS) {
                return it.copy(repCount = currentReps)
            }
        }

        // Only check if person is completely out of frame
        val visibilityMessage = checkVisibility(pose)
        if (visibilityMessage != null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = visibilityMessage,
                confidence = 0f,
                repCount = currentReps
            )
        }

        // Check confidence threshold
        val confidence = calculateConfidence(pose)
        if (confidence < MIN_CONFIDENCE_THRESHOLD) {
            // Only show "move closer" message if confidence is very low
            if (confidence < 0.15f) {
                return ExerciseFeedback(
                    isCorrectForm = false,
                    message = "Please move closer to the camera",
                    confidence = confidence,
                    repCount = currentReps
                )
            }
        }

        // Analyze based on exercise type
        val feedback = when (exerciseType) {
            ExerciseType.SQUAT -> analyzeSquat(pose, currentReps)
            ExerciseType.BENCH_PRESS -> analyzeBenchPress(pose, currentReps)
            ExerciseType.DEADLIFT -> analyzeDeadlift(pose, currentReps)
            ExerciseType.POSTURE -> analyzePosture(pose, currentReps)
            ExerciseType.SHOULDER_PRESS -> analyzeShoulderPress(pose, currentReps)
            ExerciseType.BICEP_CURL -> analyzeBicepCurl(pose, currentReps)
            ExerciseType.TRICEP_EXTENSION -> analyzeTricepExtension(pose, currentReps)
            ExerciseType.WARRIOR_POSE -> analyzeWarriorPose(pose, currentReps)
            ExerciseType.TREE_POSE -> analyzeTreePose(pose, currentReps)
            ExerciseType.DOWNWARD_DOG -> analyzeDownwardDog(pose, currentReps)
            ExerciseType.COBRA_POSE -> analyzeCobraPose(pose, currentReps)
            ExerciseType.CHAIR_POSE -> analyzeChairPose(pose, currentReps)
            ExerciseType.PLANK -> analyzePlank(pose, currentReps)
        }

        // Apply smoothing to feedback
        val smoothedFeedback = smoothFeedback(feedback)

        // Cache the feedback
        lastFeedbackTime = currentTime
        lastFeedback = smoothedFeedback

        return smoothedFeedback
    }

    private fun analyzePosture(pose: Pose, repCount: Int): ExerciseFeedback {
        // Get relevant landmarks
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null || nose == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure your upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val spineAngle = calculateSpineAngle(pose)
        val shoulderAngle = calculateShoulderAngle(pose)
        val neckAngle = calculateNeckAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check spine alignment (should be close to vertical)
        if (abs(spineAngle - 90) > 10) {
            formIssues.add("Straighten your back")
        }

        // Check shoulder alignment (should be level)
        if (abs(shoulderAngle) > 10) {
            formIssues.add("Level your shoulders")
        }

        // Check neck position (should be aligned with spine)
        if (abs(neckAngle - spineAngle) > 15) {
            formIssues.add("Align your neck with your spine")
        }

        val message = if (formIssues.isEmpty()) {
            "Good posture!"
        } else {
            formIssues.joinToString("\n")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = message,
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeSquat(pose: Pose, repCount: Int): ExerciseFeedback {
        // Get relevant landmarks
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        if (leftHip == null || leftKnee == null || leftAnkle == null ||
            rightHip == null || rightKnee == null || rightAnkle == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure your full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        // Calculate angles
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val hipAngle = calculateHipAngle(pose)
        val backAngle = calculateSpineAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check knee angles
        if (leftKneeAngle !in targetAngles["knee"]!! || rightKneeAngle !in targetAngles["knee"]!!) {
            formIssues.add("Bend your knees more - aim for parallel")
        }

        // Check hip hinge
        if (hipAngle !in targetAngles["hip"]!!) {
            formIssues.add("Keep your hips back")
        }

        // Check back position
        if (backAngle !in targetAngles["back"]!!) {
            formIssues.add("Keep your back straight")
        }

        // Check symmetry
        if (abs(leftKneeAngle - rightKneeAngle) > 15f) {
            formIssues.add("Keep your knees even")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeBenchPress(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder == null || leftElbow == null || leftWrist == null ||
            rightShoulder == null || rightElbow == null || rightWrist == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val shoulderAngle = calculateShoulderAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check elbow symmetry
        if (abs(leftElbowAngle - rightElbowAngle) > targetAngles["symmetry"]!!.start) {
            formIssues.add("Keep your arms even - one side is lower than the other")
        }

        // Check elbow angles
        if (leftElbowAngle !in targetAngles["elbow"]!! ||
            rightElbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Lower the bar until your elbows are at 90 degrees")
        }

        // Check shoulder position
        if (shoulderAngle !in targetAngles["shoulder"]!!) {
            formIssues.add("Keep your shoulders back and stable")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeDeadlift(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (leftShoulder == null || leftHip == null || leftKnee == null || leftAnkle == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val hipAngle = calculateHipAngle(pose)
        val kneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val backAngle = calculateSpineAngle(pose)
        val shoulderAngle = calculateShoulderAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check back straightness
        if (backAngle !in targetAngles["back"]!!) {
            formIssues.add("Keep your back straight")
        }

        // Check hip hinge
        if (hipAngle !in targetAngles["hip"]!!) {
            formIssues.add("Hinge at your hips more")
        }

        // Check knee position
        if (kneeAngle !in targetAngles["knee"]!!) {
            formIssues.add("Adjust knee bend - too much or too little")
        }

        // Check shoulder position
        if (shoulderAngle !in targetAngles["shoulder"]!!) {
            formIssues.add("Keep shoulders over the bar")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeShoulderPress(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder == null || leftElbow == null || leftWrist == null ||
            rightShoulder == null || rightElbow == null || rightWrist == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val shoulderAngle = calculateShoulderAngle(pose)
        val backAngle = calculateSpineAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check elbow angles
        if (leftElbowAngle !in targetAngles["elbow"]!! ||
            rightElbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Keep elbows at 90 degrees at bottom")
        }

        // Check shoulder position
        if (shoulderAngle !in targetAngles["shoulder"]!!) {
            formIssues.add("Keep shoulders stable and aligned")
        }

        // Check back position
        if (backAngle !in targetAngles["back"]!!) {
            formIssues.add("Maintain straight back")
        }

        // Check symmetry
        if (abs(leftElbowAngle - rightElbowAngle) > targetAngles["symmetry"]!!.start) {
            formIssues.add("Keep arms even")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeBicepCurl(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder == null || leftElbow == null || leftWrist == null ||
            rightShoulder == null || rightElbow == null || rightWrist == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val shoulderAngle = calculateShoulderAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check elbow angles
        if (leftElbowAngle !in targetAngles["elbow"]!! ||
            rightElbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Keep elbows close to body")
        }

        // Check shoulder stability
        if (shoulderAngle !in targetAngles["shoulder"]!!) {
            formIssues.add("Keep shoulders still - no swinging")
        }

        // Check symmetry
        if (abs(leftElbowAngle - rightElbowAngle) > targetAngles["symmetry"]!!.start) {
            formIssues.add("Keep curls even on both sides")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeTricepExtension(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder == null || leftElbow == null || leftWrist == null ||
            rightShoulder == null || rightElbow == null || rightWrist == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val shoulderAngle = calculateShoulderAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check elbow extension
        if (leftElbowAngle !in targetAngles["elbow"]!! ||
            rightElbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Extend arms fully behind head")
        }

        // Check shoulder position
        if (shoulderAngle !in targetAngles["shoulder"]!!) {
            formIssues.add("Keep upper arms close to ears")
        }

        // Check symmetry
        if (abs(leftElbowAngle - rightElbowAngle) > targetAngles["symmetry"]!!.start) {
            formIssues.add("Keep extension even on both sides")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeWarriorPose(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftAnkle == null || leftKnee == null || leftHip == null ||
            rightAnkle == null || rightKnee == null || rightHip == null ||
            leftShoulder == null || rightShoulder == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val frontKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val backLegAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val hipAlignment = calculateHipAngle(pose)
        val torsoAngle = calculateSpineAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check front knee angle (should be 90 degrees)
        if (frontKneeAngle !in targetAngles["front_knee"]!!) {
            formIssues.add("Bend front knee to 90 degrees")
        }

        // Check back leg straightness
        if (backLegAngle !in targetAngles["back_leg"]!!) {
            formIssues.add("Keep back leg straight")
        }

        // Check hip alignment
        if (hipAlignment !in targetAngles["hips"]!!) {
            formIssues.add("Square your hips")
        }

        // Check torso alignment
        if (torsoAngle !in targetAngles["torso"]!!) {
            formIssues.add("Keep torso upright")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good warrior pose!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeTreePose(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val spine = calculateSpineAngle(pose)

        if (leftAnkle == null || leftKnee == null || leftHip == null || rightHip == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val standingLegAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val hipLevel = abs(leftHip.position.y - rightHip.position.y)

        val formIssues = mutableListOf<String>()

        // Check standing leg straightness
        if (standingLegAngle !in targetAngles["standing_leg"]!!) {
            formIssues.add("Keep standing leg straight")
        }

        // Check hip levelness
        if (hipLevel > targetAngles["hip_level"]!!.start) {
            formIssues.add("Keep hips level")
        }

        // Check spine alignment
        if (spine !in targetAngles["spine"]!!) {
            formIssues.add("Keep spine straight")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good tree pose!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeDownwardDog(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (leftShoulder == null || leftHip == null || leftKnee == null || leftAnkle == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val spineAngle = calculateSpineAngle(pose)
        val legAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val armAlignment = calculateShoulderAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check spine alignment
        if (spineAngle !in targetAngles["spine"]!!) {
            formIssues.add("Lengthen spine, push hips up and back")
        }

        // Check leg straightness
        if (legAngle !in targetAngles["legs"]!!) {
            formIssues.add("Straighten legs")
        }

        // Check arm alignment
        if (armAlignment !in targetAngles["arms"]!!) {
            formIssues.add("Keep arms straight and shoulder-width apart")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good downward dog!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeCobraPose(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val spine = calculateSpineAngle(pose)

        if (leftShoulder == null || leftElbow == null || leftHip == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure upper body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val elbowAngle = calculateAngle(leftShoulder, leftElbow, leftHip)
        val shoulderHeight = leftShoulder.position.y - leftHip.position.y

        val formIssues = mutableListOf<String>()

        // Check elbow bend
        if (elbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Keep elbows slightly bent")
        }

        // Check shoulder height
        if (shoulderHeight > targetAngles["shoulder_height"]!!.start) {
            formIssues.add("Lower shoulders away from ears")
        }

        // Check spine extension
        if (spine !in targetAngles["spine"]!!) {
            formIssues.add("Lift chest while keeping lower body grounded")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good cobra pose!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzeChairPose(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val spine = calculateSpineAngle(pose)

        if (leftHip == null || leftKnee == null || leftAnkle == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val kneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val hipAngle = calculateHipAngle(pose)

        val formIssues = mutableListOf<String>()

        // Check knee angle
        if (kneeAngle !in targetAngles["knee"]!!) {
            formIssues.add("Bend knees to 90 degrees")
        }

        // Check hip position
        if (hipAngle !in targetAngles["hip"]!!) {
            formIssues.add("Lower hips as if sitting back")
        }

        // Check spine alignment
        if (spine !in targetAngles["spine"]!!) {
            formIssues.add("Keep chest lifted, spine straight")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good chair pose!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun analyzePlank(pose: Pose, repCount: Int): ExerciseFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        if (leftShoulder == null || leftElbow == null || leftHip == null ||
            leftKnee == null || leftAnkle == null) {
            return ExerciseFeedback(
                isCorrectForm = false,
                message = "Please ensure full body is visible",
                confidence = 0f,
                repCount = repCount
            )
        }

        val spineAngle = calculateSpineAngle(pose)
        val elbowAngle = calculateAngle(leftShoulder, leftElbow, leftHip)
        val hipHeight = abs(leftHip.position.y - leftShoulder.position.y)

        val formIssues = mutableListOf<String>()

        // Check body alignment
        if (spineAngle !in targetAngles["spine"]!!) {
            formIssues.add("Keep body in straight line")
        }

        // Check elbow position
        if (elbowAngle !in targetAngles["elbow"]!!) {
            formIssues.add("Keep elbows under shoulders")
        }

        // Check hip height
        if (hipHeight > targetAngles["hip_height"]!!.start) {
            formIssues.add("Keep hips level with shoulders")
        }

        return ExerciseFeedback(
            isCorrectForm = formIssues.isEmpty(),
            message = if (formIssues.isEmpty()) "Good plank form!" else formIssues.joinToString("\n"),
            confidence = calculateConfidence(pose),
            repCount = repCount
        )
    }

    private fun calculateHipAngle(pose: Pose): Float {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

        if (leftHip == null || leftKnee == null || leftShoulder == null) {
            return 0f
        }

        return calculateAngle(leftShoulder, leftHip, leftKnee)
    }

    private fun calculateNeckAngle(pose: Pose): Float {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (nose == null || leftShoulder == null || rightShoulder == null) {
            return 0f
        }

        val midShoulder = Point(
            (leftShoulder.position.x + rightShoulder.position.x) / 2,
            (leftShoulder.position.y + rightShoulder.position.y) / 2
        )

        val deltaX = nose.position.x - midShoulder.x
        val deltaY = nose.position.y - midShoulder.y
        return Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat() + 90
    }

    private fun calculateShoulderAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val neck = pose.getPoseLandmark(PoseLandmark.NOSE)

        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null || neck == null) {
            return 0f
        }

        val midShoulder = Point(
            (leftShoulder.position.x + rightShoulder.position.x) / 2,
            (leftShoulder.position.y + rightShoulder.position.y) / 2
        )

        val midHip = Point(
            (leftHip.position.x + rightHip.position.x) / 2,
            (leftHip.position.y + rightHip.position.y) / 2
        )

        // Calculate angle between neck, midShoulder and midHip
        val angle = Math.toDegrees(
            (atan2(
                midHip.y - midShoulder.y,
                midHip.x - midShoulder.x
            ) - atan2(
                neck.position.y - midShoulder.y,
                neck.position.x - midShoulder.x
            )).toDouble()
        ).toFloat()

        return abs(angle)
    }

    private fun calculateSpineAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null) {
            return 0f
        }

        val midShoulder = Point(
            (leftShoulder.position.x + rightShoulder.position.x) / 2,
            (leftShoulder.position.y + rightShoulder.position.y) / 2
        )

        val midHip = Point(
            (leftHip.position.x + rightHip.position.x) / 2,
            (leftHip.position.y + rightHip.position.y) / 2
        )

        val deltaX = midHip.x - midShoulder.x
        val deltaY = midHip.y - midShoulder.y
        return Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat() + 90
    }

    private fun calculateAngle(
        first: PoseLandmark,
        middle: PoseLandmark,
        last: PoseLandmark
    ): Float {
        val angle = Math.toDegrees(
            (atan2(
                last.position.y - middle.position.y,
                last.position.x - middle.position.x
            ) - atan2(
                first.position.y - middle.position.y,
                first.position.x - middle.position.x
            )).toDouble()
        ).toFloat()

        return abs(angle)
    }

    private fun checkVisibility(pose: Pose): String? {
        // Only check if the person is completely out of frame
        val visibleLandmarks = pose.allPoseLandmarks.count {
            it.inFrameLikelihood > MIN_VISIBILITY_THRESHOLD
        }

        if (visibleLandmarks < 3) { // If less than 3 landmarks are visible
            return "Please step into the frame"
        }
        return null
    }

    private fun calculateConfidence(pose: Pose): Float {
        // Get relevant landmarks based on exercise type
        val keyLandmarks = when (exerciseType) {
            ExerciseType.SQUAT, ExerciseType.DEADLIFT -> listOf(
                // Lower body focus
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,

            )

            ExerciseType.BENCH_PRESS, ExerciseType.SHOULDER_PRESS -> listOf(
                // Upper body focus
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            )

            ExerciseType.BICEP_CURL, ExerciseType.TRICEP_EXTENSION -> listOf(
                // Arm focus
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST
            )

            ExerciseType.WARRIOR_POSE, ExerciseType.TREE_POSE -> listOf(
                // Full body focus
                PoseLandmark.NOSE,
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
            )

            ExerciseType.PLANK -> listOf(
                // Core and alignment focus
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            )

            ExerciseType.POSTURE -> listOf(
                // Upper body alignment focus

                PoseLandmark.LEFT_EYE, PoseLandmark.RIGHT_EYE,
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            )

            else -> pose.allPoseLandmarks.map { it.landmarkType }
        }

        // Calculate confidence based on visibility and likelihood of key landmarks
        val confidenceValues = keyLandmarks.mapNotNull { landmarkType ->
            pose.getPoseLandmark(landmarkType)?.let { landmark ->
                // Consider both in-frame likelihood and tracking confidence
                val inFrameConfidence = landmark.inFrameLikelihood
                val trackingConfidence = if (landmark.position3D != null) {
                    landmark.position3D.z // Depth confidence
                } else {
                    1.0f
                }
                inFrameConfidence * trackingConfidence
            }
        }

        // Return average confidence, or 0 if no landmarks detected
        return if (confidenceValues.isEmpty()) {
            0f
        } else {
            // Take the average of the top 75% confidence values
            confidenceValues.sortedDescending()
                .take((confidenceValues.size * 0.75).toInt())
                .average()
                .toFloat()
                .coerceIn(0f, 1f)
        }
    }

    private fun smoothFeedback(feedback: ExerciseFeedback): ExerciseFeedback {
        if (feedback.isCorrectForm) return feedback

        // Split current feedback messages
        val currentFeedbackItems = feedback.message.split("\n")

        // If no previous feedback exists, limit messages and return
        if (lastFeedback == null) {
            return feedback.copy(
                message = currentFeedbackItems.take(MAX_FEEDBACK_MESSAGES).joinToString("\n")
            )
        }

        // Split previous feedback messages
        val lastFeedbackItems = lastFeedback!!.message.split("\n")

        // Prioritize messages in this order:
        // 1. Critical form issues that persist from last feedback
        // 2. New critical form issues
        // 3. Less critical form issues
        val prioritizedMessages = mutableListOf<String>()

        // First, add persistent critical issues
        val persistentIssues = currentFeedbackItems.filter { current ->
            lastFeedbackItems.any { last ->
                last.equals(current, ignoreCase = true)
            }
        }

        // Critical form issues keywords
        val criticalKeywords = setOf(
            "back", "spine", "straight",
            "knee", "shoulder", "neck",
            "head", "posture", "alignment"
        )

        // Add persistent critical issues first
        persistentIssues.forEach { issue ->
            if (criticalKeywords.any { keyword ->
                    issue.contains(keyword, ignoreCase = true)
                }) {
                prioritizedMessages.add(issue)
            }
        }

        // Then add new critical issues
        currentFeedbackItems
            .filter { !persistentIssues.contains(it) }
            .forEach { issue ->
                if (criticalKeywords.any { keyword ->
                        issue.contains(keyword, ignoreCase = true)
                    }) {
                    prioritizedMessages.add(issue)
                }
            }

        // Finally, add remaining issues if we have space
        if (prioritizedMessages.size < MAX_FEEDBACK_MESSAGES) {
            val remainingSlots = MAX_FEEDBACK_MESSAGES - prioritizedMessages.size
            val remainingIssues = currentFeedbackItems.filter { issue ->
                !prioritizedMessages.contains(issue) &&
                        !criticalKeywords.any { keyword ->
                            issue.contains(keyword, ignoreCase = true)
                        }
            }
            prioritizedMessages.addAll(remainingIssues.take(remainingSlots))
        }

        // Ensure we don't exceed maximum messages
        val finalMessages = prioritizedMessages.take(MAX_FEEDBACK_MESSAGES)

        return if (finalMessages.isEmpty()) {
            feedback.copy(message = "Keep going!")
        } else {
            feedback.copy(message = finalMessages.joinToString("\n"))
        }
    }

    data class Point(val x: Float, val y: Float)

    companion object {
        private const val TAG = "ExerciseAnalyzer"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.3f  // Reduced from 0.5f
        private const val MIN_VISIBILITY_THRESHOLD = 0.1f  // New threshold for visibility check
        private const val MAX_FEEDBACK_MESSAGES = 2 // Maximum number of feedback messages to show at once
    }
}