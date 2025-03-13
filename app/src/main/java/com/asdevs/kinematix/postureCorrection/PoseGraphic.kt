package com.asdevs.kinematix.postureCorrection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.asdevs.kinematix.models.ExerciseCategory
import com.asdevs.kinematix.models.ExerciseType
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.atan2

private fun Double.abs(): Double = kotlin.math.abs(this)


class PoseGraphic(
    overlay: GraphicOverlay,
    private val pose: Pose,
    private val exerciseType: ExerciseType,
    private val showInFrameLikelihood: Boolean,
    private val isGoodPosture: Boolean = true
) : GraphicOverlay.Graphic(overlay) {

    private val goodPosturePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val badPosturePaint = Paint().apply {
        color = Color.RED
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val leftPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = STROKE_WIDTH
    }
    private val rightPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = STROKE_WIDTH
    }

    private val guidelinePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    private val currentPaint get() = if (isGoodPosture) goodPosturePaint else badPosturePaint


    override fun draw(canvas: Canvas) {
        if (pose.allPoseLandmarks.isEmpty()) return

        // Draw all landmarks
        pose.allPoseLandmarks.forEach { landmark ->
            drawLandmark(canvas, landmark)
        }

        // Draw connections based on exercise type
        when (exerciseType.getCategory()) {
            ExerciseCategory.STRENGTH -> drawStrengthConnections(canvas)
            ExerciseCategory.YOGA -> drawYogaConnections(canvas)
            ExerciseCategory.GENERAL -> drawGeneralConnections(canvas)
        }

        // Draw exercise-specific guidelines
        drawExerciseGuidelines(canvas)
    }

    private fun drawLandmark(canvas: Canvas, landmark: PoseLandmark) {
        val paint = getPaint(landmark)
        canvas.drawCircle(
            translateX(landmark.position.x),
            translateY(landmark.position.y),
            DOT_RADIUS,
            paint
        )
    }

    private fun drawStrengthConnections(canvas: Canvas) {
        when (exerciseType) {
            ExerciseType.SQUAT -> {
                drawBasicConnections(canvas)
                drawSquatGuidelines(canvas)
            }
            ExerciseType.BENCH_PRESS -> {
                drawUpperBodyConnections(canvas)
                drawBenchPressGuidelines(canvas)
            }
            ExerciseType.DEADLIFT -> {
                drawBasicConnections(canvas)
                drawDeadliftGuidelines(canvas)
            }
            else -> drawBasicConnections(canvas)
        }
    }

    private fun drawYogaConnections(canvas: Canvas) {
        when (exerciseType) {
            ExerciseType.WARRIOR_POSE -> {
                drawBasicConnections(canvas)
                drawWarriorPoseGuidelines(canvas)
            }
            ExerciseType.TREE_POSE -> {
                drawBasicConnections(canvas)
                drawTreePoseGuidelines(canvas)
            }
            else -> drawBasicConnections(canvas)
        }
    }

    private fun drawBasicConnections(canvas: Canvas) {
        // Left side
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, leftPaint)

        // Right side
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, rightPaint)

        // Center lines
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, currentPaint)
    }

    private fun drawExerciseGuidelines(canvas: Canvas) {
        when (exerciseType) {
            ExerciseType.SQUAT -> drawSquatGuidelines(canvas)
            ExerciseType.BENCH_PRESS -> drawBenchPressGuidelines(canvas)
            ExerciseType.WARRIOR_POSE -> drawWarriorPoseGuidelines(canvas)
            ExerciseType.TREE_POSE -> drawTreePoseGuidelines(canvas)
            else -> {} // No specific guidelines
        }
    }

    private fun drawSquatGuidelines(canvas: Canvas) {
        // Draw knee alignment guides
        pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.let { knee ->
            val x = translateX(knee.position.x)
            canvas.drawLine(x, 0f, x, canvas.height.toFloat(), guidelinePaint)
        }

        // Draw hip depth guide
        pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.let { hip ->
            val y = translateY(hip.position.y)
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, guidelinePaint)
        }
    }

    private fun drawBenchPressGuidelines(canvas: Canvas) {
        // Draw bar path guide
        pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.let { wrist ->
            val x = translateX(wrist.position.x)
            canvas.drawLine(x, 0f, x, canvas.height.toFloat(), guidelinePaint)
        }

        // Draw chest level guide
        pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.let { shoulder ->
            val y = translateY(shoulder.position.y)
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, guidelinePaint)
        }
    }

    private fun drawWarriorPoseGuidelines(canvas: Canvas) {
        // Draw alignment guides for warrior pose
        pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.let { hip ->
            val y = translateY(hip.position.y)
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, guidelinePaint)
        }
    }

    private fun drawTreePoseGuidelines(canvas: Canvas) {
        // Draw vertical alignment guide for balance
        pose.getPoseLandmark(PoseLandmark.NOSE)?.let { nose ->
            val x = translateX(nose.position.x)
            canvas.drawLine(x, 0f, x, canvas.height.toFloat(), guidelinePaint)
        }
    }

    private fun getPaint(landmark: PoseLandmark): Paint {
        return when {
            landmark.position3D.z < 0 -> leftPaint
            else -> rightPaint
        }
    }

    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Double {
        val angle = Math.toDegrees(
            (atan2(last.position.y - middle.position.y,
                last.position.x - middle.position.x) -
                    atan2(first.position.y - middle.position.y,
                        first.position.x - middle.position.x)).toDouble()
        ).abs()
        return if (angle > 180) 360 - angle else angle
    }

    private fun drawLineIfLandmarksExist(
        canvas: Canvas,
        startLandmarkType: Int,
        endLandmarkType: Int,
        paint: Paint
    ) {
        val start = pose.getPoseLandmark(startLandmarkType)
        val end = pose.getPoseLandmark(endLandmarkType)

        if (start != null && end != null) {
            canvas.drawLine(
                translateX(start.position.x),
                translateY(start.position.y),
                translateX(end.position.x),
                translateY(end.position.y),
                paint
            )
        }
    }

    private fun drawGeneralConnections(canvas: Canvas) {
        // Draw basic body connections
        drawBasicConnections(canvas)

        // Additional connections for general posture
        drawLineIfLandmarksExist(canvas, PoseLandmark.NOSE, PoseLandmark.LEFT_EYE_OUTER, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_EYE_OUTER, PoseLandmark.LEFT_EYE, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EYE_INNER, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.NOSE, PoseLandmark.RIGHT_EYE_OUTER, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_EYE_OUTER, PoseLandmark.RIGHT_EYE, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EYE_INNER, currentPaint)

        // Draw neck line
        drawLineIfLandmarksExist(canvas, PoseLandmark.NOSE, PoseLandmark.LEFT_SHOULDER, currentPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.NOSE, PoseLandmark.RIGHT_SHOULDER, currentPaint)

        // Draw spine line
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            val midShoulderX = (leftShoulder.position.x + rightShoulder.position.x) / 2
            val midShoulderY = (leftShoulder.position.y + rightShoulder.position.y) / 2
            val midHipX = (leftHip.position.x + rightHip.position.x) / 2
            val midHipY = (leftHip.position.y + rightHip.position.y) / 2

            canvas.drawLine(
                translateX(midShoulderX),
                translateY(midShoulderY),
                translateX(midHipX),
                translateY(midHipY),
                currentPaint
            )
        }
    }

    private fun drawUpperBodyConnections(canvas: Canvas) {
        // Draw shoulder connections
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, currentPaint)

        // Draw arm connections
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, rightPaint)

        // Draw chest/torso connections
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, leftPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, rightPaint)
        drawLineIfLandmarksExist(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, currentPaint)

        // Draw wrist alignment guides for bench press
        if (exerciseType == ExerciseType.BENCH_PRESS) {
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.let { leftWrist ->
                pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.let { rightWrist ->
                    val midY = (leftWrist.position.y + rightWrist.position.y) / 2
                    canvas.drawLine(
                        0f,
                        translateY(midY),
                        canvas.width.toFloat(),
                        translateY(midY),
                        guidelinePaint
                    )
                }
            }
        }
    }

    private fun drawDeadliftGuidelines(canvas: Canvas) {
        // Draw vertical bar path
        pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.let { ankle ->
            val x = translateX(ankle.position.x + 20) // Slightly in front of ankles
            canvas.drawLine(x, 0f, x, canvas.height.toFloat(), guidelinePaint)
        }

        // Draw hip hinge guide
        pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.let { hip ->
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.let { shoulder ->
                // Draw arc for hip hinge movement
                val paint = Paint(guidelinePaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = STROKE_WIDTH / 2
                }

                val radius = Math.abs(shoulder.position.y - hip.position.y)
                canvas.drawArc(
                    translateX(hip.position.x - radius),
                    translateY(hip.position.y - radius),
                    translateX(hip.position.x + radius),
                    translateY(hip.position.y + radius),
                    0f,
                    90f,
                    false,
                    paint
                )
            }
        }

        // Draw shin angle guide
        pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.let { ankle ->
            pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.let { knee ->
                val shinAngle = calculateAngle(
                    ankle,
                    knee,
                    pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return@let
                )

                // Draw guideline for optimal shin angle (around 75 degrees)
                if (shinAngle < 70 || shinAngle > 80) {
                    val paint = Paint(guidelinePaint).apply {
                        color = if (shinAngle < 70) Color.RED else Color.YELLOW
                    }
                    canvas.drawLine(
                        translateX(ankle.position.x),
                        translateY(ankle.position.y),
                        translateX(knee.position.x),
                        translateY(knee.position.y),
                        paint
                    )
                }
            }
        }

        // Draw back angle guide
        pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.let { shoulder ->
            pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.let { hip ->
                val backAngle = calculateAngle(
                    shoulder,
                    hip,
                    pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return@let
                )

                // Draw guideline for optimal back angle (around 45 degrees at bottom)
                if (backAngle < 40 || backAngle > 50) {
                    val paint = Paint(guidelinePaint).apply {
                        color = if (backAngle < 40) Color.RED else Color.YELLOW
                    }
                    canvas.drawLine(
                        translateX(hip.position.x),
                        translateY(hip.position.y),
                        translateX(shoulder.position.x),
                        translateY(shoulder.position.y),
                        paint
                    )
                }
            }
        }
    }

    private fun drawAngleArc(
        canvas: Canvas,
        point1: PoseLandmark,
        vertex: PoseLandmark,
        point2: PoseLandmark,
        paint: Paint
    ) {
        val angle = calculateAngle(point1, vertex, point2)
        val radius = 30f // Radius of the arc

        val startAngle = Math.toDegrees(
            atan2(
                (point1.position.y - vertex.position.y).toDouble(),
                (point1.position.x - vertex.position.x).toDouble()
            )
        ).toFloat()

        canvas.drawArc(
            translateX(vertex.position.x - radius),
            translateY(vertex.position.y - radius),
            translateX(vertex.position.x + radius),
            translateY(vertex.position.y + radius),
            startAngle,
            angle.toFloat(),
            false,
            paint
        )
    }

    companion object {
        private const val DOT_RADIUS = 8.0f
        private const val STROKE_WIDTH = 4.0f
        private const val GUIDELINE_ALPHA = 128
    }
}