package com.asdevs.kinematix.postureCorrection

import com.asdevs.kinematix.models.ExerciseType
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.atan2
import kotlin.math.abs

class RepCounter(private val exerciseType: ExerciseType) {
    private var repCount = 0
    private var isInBottomPosition = false
    private var lastValidPosition = 0f
    private var lastProcessedTime = 0L
    private var lastAngle = 0f
    private val PROCESS_THROTTLE_MS = 100 // Prevent too frequent updates
    private val MIN_ANGLE_CHANGE = 5f // Minimum angle change to count as movement

    fun processPosition(pose: Pose): Int {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < PROCESS_THROTTLE_MS) {
            return repCount
        }
        lastProcessedTime = currentTime

        return when (exerciseType) {
            ExerciseType.SQUAT -> {
                val kneeAngle = calculateKneeAngle(pose)
                if (kneeAngle > 0) {
                    processRep(kneeAngle, SQUAT_TOP, SQUAT_BOTTOM)
                } else repCount
            }
            ExerciseType.BENCH_PRESS -> {
                val elbowAngle = calculateElbowAngle(pose)
                if (elbowAngle > 0) {
                    processRep(elbowAngle, BENCH_PRESS_TOP, BENCH_PRESS_BOTTOM)
                } else repCount
            }
            ExerciseType.DEADLIFT -> {
                val hipAngle = calculateHipAngle(pose)
                if (hipAngle > 0) {
                    processRep(hipAngle, DEADLIFT_TOP, DEADLIFT_BOTTOM)
                } else repCount
            }
            ExerciseType.SHOULDER_PRESS -> {
                val shoulderAngle = calculateShoulderAngle(pose)
                if (shoulderAngle > 0) {
                    processRep(shoulderAngle, SHOULDER_PRESS_TOP, SHOULDER_PRESS_BOTTOM)
                } else repCount
            }
            ExerciseType.BICEP_CURL -> {
                val elbowAngle = calculateElbowAngle(pose)
                if (elbowAngle > 0) {
                    processRep(elbowAngle, BICEP_CURL_TOP, BICEP_CURL_BOTTOM)
                } else repCount
            }
            ExerciseType.TRICEP_EXTENSION -> {
                val elbowAngle = calculateElbowAngle(pose)
                if (elbowAngle > 0) {
                    processRep(elbowAngle, TRICEP_EXTENSION_TOP, TRICEP_EXTENSION_BOTTOM)
                } else repCount
            }
            else -> repCount // For poses that don't count reps
        }
    }

    private fun calculateKneeAngle(pose: Pose): Float {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftAngle = if (leftHip != null && leftKnee != null && leftAnkle != null) {
            calculateAngle(leftHip, leftKnee, leftAnkle)
        } else 0f

        val rightAngle = if (rightHip != null && rightKnee != null && rightAnkle != null) {
            calculateAngle(rightHip, rightKnee, rightAnkle)
        } else 0f

        return if (leftAngle > 0 && rightAngle > 0) (leftAngle + rightAngle) / 2
        else maxOf(leftAngle, rightAngle)
    }

    private fun calculateElbowAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            calculateAngle(leftShoulder, leftElbow, leftWrist)
        } else 0f

        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else 0f

        return if (leftAngle > 0 && rightAngle > 0) (leftAngle + rightAngle) / 2
        else maxOf(leftAngle, rightAngle)
    }

    private fun calculateHipAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val leftAngle = if (leftShoulder != null && leftHip != null && leftKnee != null) {
            calculateAngle(leftShoulder, leftHip, leftKnee)
        } else 0f

        val rightAngle = if (rightShoulder != null && rightHip != null && rightKnee != null) {
            calculateAngle(rightShoulder, rightHip, rightKnee)
        } else 0f

        return if (leftAngle > 0 && rightAngle > 0) (leftAngle + rightAngle) / 2
        else maxOf(leftAngle, rightAngle)
    }

    private fun calculateShoulderAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            calculateAngle(leftShoulder, leftElbow, leftWrist)
        } else 0f

        val rightAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else 0f

        return if (leftAngle > 0 && rightAngle > 0) (leftAngle + rightAngle) / 2
        else maxOf(leftAngle, rightAngle)
    }

    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Float {
        val angle = Math.toDegrees(
            (atan2(last.position.y - middle.position.y,
                last.position.x - middle.position.x) -
                    atan2(first.position.y - middle.position.y,
                        first.position.x - middle.position.x)).toDouble()
        ).toFloat()
        return abs(angle)
    }

    private fun processRep(currentAngle: Float, topThreshold: Float, bottomThreshold: Float): Int {
        // Ignore small fluctuations
        if (abs(currentAngle - lastAngle) < MIN_ANGLE_CHANGE) {
            return repCount
        }
        lastAngle = currentAngle

        if (!isInBottomPosition && currentAngle <= bottomThreshold) {
            isInBottomPosition = true
            lastValidPosition = currentAngle
        } else if (isInBottomPosition && currentAngle >= topThreshold) {
            isInBottomPosition = false
            lastValidPosition = currentAngle
            repCount++
        }
        return repCount
    }

    fun getRepCount(): Int = repCount

    fun reset() {
        repCount = 0
        isInBottomPosition = false
        lastValidPosition = 0f
        lastAngle = 0f
    }

    companion object {
        // Angle thresholds for different exercises
        private const val SQUAT_TOP = 160f
        private const val SQUAT_BOTTOM = 100f
        private const val BENCH_PRESS_TOP = 165f
        private const val BENCH_PRESS_BOTTOM = 90f
        private const val DEADLIFT_TOP = 165f
        private const val DEADLIFT_BOTTOM = 90f
        private const val SHOULDER_PRESS_TOP = 170f
        private const val SHOULDER_PRESS_BOTTOM = 90f
        private const val BICEP_CURL_TOP = 160f
        private const val BICEP_CURL_BOTTOM = 45f
        private const val TRICEP_EXTENSION_TOP = 160f
        private const val TRICEP_EXTENSION_BOTTOM = 60f
    }
}