import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.asdevs.kinematix.models.ExerciseType
import com.asdevs.kinematix.postureCorrection.GraphicOverlay
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class ExerciseGraphic(
    overlay: GraphicOverlay,
    private val pose: Pose,
    private val exerciseType: ExerciseType
) : GraphicOverlay.Graphic(overlay) {

    private val leftPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val rightPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
    }

    private val goodPosturePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val neutralPosturePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val badPosturePaint = Paint().apply {
        color = Color.RED
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        // Clear any previous drawings
        canvas.save()
        // Draw white background circles for better visibility
        pose.allPoseLandmarks.forEach { landmark ->
            val point = translatePoseLandmark(landmark)
            canvas.drawCircle(point.x, point.y, DOT_RADIUS + 2, whitePaint)
        }

        // Draw landmarks with confidence-based colors
        pose.allPoseLandmarks.forEach { landmark ->
            val point = translatePoseLandmark(landmark)
            val paint = when {
                landmark.inFrameLikelihood > 0.9f -> goodPosturePaint
                landmark.inFrameLikelihood > 0.5f -> neutralPosturePaint
                else -> badPosturePaint
            }
            canvas.drawCircle(
                point.x, point.y,
                if (landmark.inFrameLikelihood > 0.5f) DOT_RADIUS else DOT_RADIUS * 0.5f,
                paint
            )
        }

        // Draw connections between landmarks
        drawExerciseSpecificConnections(canvas)
        // Draw landmarks
        pose.allPoseLandmarks.forEach { landmark ->
            val point = translatePoseLandmark(landmark)
            canvas.drawCircle(point.x, point.y, DOT_RADIUS, whitePaint)
        }

        canvas.restore()
    }

    private fun translatePoseLandmark(landmark: PoseLandmark): Point {
        return Point(
            translateX(landmark.position.x),
            translateY(landmark.position.y)
        )
    }

    data class Point(val x: Float, val y: Float)

    private fun drawExerciseSpecificConnections(canvas: Canvas) {
        when (exerciseType) {
            ExerciseType.SQUAT -> drawSquatConnections(canvas)
            ExerciseType.BENCH_PRESS -> drawBenchPressConnections(canvas)
            ExerciseType.DEADLIFT -> drawDeadliftConnections(canvas)
            ExerciseType.POSTURE -> drawPostureConnections(canvas)
            ExerciseType.SHOULDER_PRESS -> drawShoulderPressConnections(canvas)
            ExerciseType.BICEP_CURL -> drawBicepCurlConnections(canvas)
            ExerciseType.TRICEP_EXTENSION -> drawTricepExtensionConnections(canvas)
            ExerciseType.WARRIOR_POSE -> drawWarriorPoseConnections(canvas)
            ExerciseType.TREE_POSE -> drawTreePoseConnections(canvas)
            ExerciseType.DOWNWARD_DOG -> drawDownwardDogConnections(canvas)
            ExerciseType.COBRA_POSE -> drawCobraPoseConnections(canvas)
            ExerciseType.CHAIR_POSE -> drawChairPoseConnections(canvas)
            ExerciseType.PLANK -> drawPlankConnections(canvas)
        }
    }

    private fun drawConnection(
        canvas: Canvas,
        startLandmark: Int,
        endLandmark: Int,
        paint: Paint? = null
    ) {
        val start = pose.getPoseLandmark(startLandmark)
        val end = pose.getPoseLandmark(endLandmark)

        if (start != null && end != null) {
            val startPoint = translatePoseLandmark(start)
            val endPoint = translatePoseLandmark(end)

            // If no specific paint is provided, determine paint based on:
            // 1. Left/Right side
            // 2. Confidence level
            val connectionPaint = paint ?: when {
                // Check if it's a left-side connection
                (startLandmark.toString().contains("LEFT") &&
                        endLandmark.toString().contains("LEFT")) -> {
                    when {
                        start.inFrameLikelihood > 0.9f && end.inFrameLikelihood > 0.9f -> leftPaint
                        start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f ->
                            neutralPosturePaint
                        else -> badPosturePaint
                    }
                }
                // Check if it's a right-side connection
                (startLandmark.toString().contains("RIGHT") &&
                        endLandmark.toString().contains("RIGHT")) -> {
                    when {
                        start.inFrameLikelihood > 0.9f && end.inFrameLikelihood > 0.9f -> rightPaint
                        start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f ->
                            neutralPosturePaint
                        else -> badPosturePaint
                    }
                }
                // For connections between left and right (like shoulders, hips)
                else -> {
                    when {
                        start.inFrameLikelihood > 0.9f && end.inFrameLikelihood > 0.9f ->
                            goodPosturePaint
                        start.inFrameLikelihood > 0.5f && end.inFrameLikelihood > 0.5f ->
                            neutralPosturePaint
                        else -> badPosturePaint
                    }
                }
            }

            canvas.drawLine(
                startPoint.x, startPoint.y,
                endPoint.x, endPoint.y,
                connectionPaint
            )
        }
    }



    private fun drawPostureConnections(canvas: Canvas) {
        // Head and neck
        drawConnection(canvas, PoseLandmark.NOSE, PoseLandmark.LEFT_EAR)
        drawConnection(canvas, PoseLandmark.NOSE, PoseLandmark.RIGHT_EAR)
        drawConnection(canvas, PoseLandmark.LEFT_EAR, PoseLandmark.LEFT_SHOULDER)
        drawConnection(canvas, PoseLandmark.RIGHT_EAR, PoseLandmark.RIGHT_SHOULDER)

        // Shoulders and upper back
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)

        // Spine and core
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)

        // Arms
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Lower body
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }

    private fun drawSquatConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawConnection(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
    }

    private fun drawBenchPressConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
    }

    private fun drawDeadliftConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
    }

    private fun drawShoulderPressConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
    }

    private fun drawBicepCurlConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
    }

    private fun drawTricepExtensionConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
    }

    private fun drawWarriorPoseConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
    }

    private fun drawTreePoseConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
    }

    private fun drawDownwardDogConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_SHOULDER)
        drawConnection(canvas, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP)
    }

    private fun drawCobraPoseConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }

    private fun drawChairPoseConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
    }

    private fun drawPlankConnections(canvas: Canvas) {
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawConnection(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawConnection(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawConnection(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawConnection(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawConnection(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawConnection(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }

    companion object {
        private const val STROKE_WIDTH = 10f
        private const val DOT_RADIUS = 8f
    }
}