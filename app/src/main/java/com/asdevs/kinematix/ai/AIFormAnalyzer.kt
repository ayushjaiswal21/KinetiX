package com.asdevs.kinematix.ai

import android.content.Context
import android.util.Log
import com.asdevs.kinematix.models.ExerciseType
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

/**
 * Enhanced AI Form Analyzer with machine learning capabilities
 * Provides personalized feedback and adaptive form correction
 */
class AIFormAnalyzer(private val context: Context) {
    
    private val personalizedFeedback = PersonalizedFeedbackEngine()
    private val formClassifier = FormClassificationModel()
    private val userHistoryManager = UserFormHistoryManager(context)
    
    companion object {
        private const val TAG = "AIFormAnalyzer"
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val LEARNING_RATE = 0.1f
    }
    
    /**
     * Analyzes pose with AI-enhanced feedback
     */
    suspend fun analyzeFormWithAI(
        pose: Pose, 
        exerciseType: ExerciseType, 
        userId: String
    ): AIFormFeedback {
        try {
            // Get user's historical form data
            val userHistory = userHistoryManager.getUserHistory(userId, exerciseType)
            
            // Advanced form classification using ML
            val formScore = formClassifier.classifyForm(pose, exerciseType)
            
            // Generate personalized feedback based on user's patterns
            val personalizedTips = personalizedFeedback.generateTips(userHistory, formScore, exerciseType)
            
            // Adaptive feedback based on user's skill progression
            val adaptiveFeedback = generateAdaptiveFeedback(userHistory, formScore)
            
            // Advanced pose analysis with biomechanical insights
            val biomechanicalAnalysis = analyzeBiomechanics(pose, exerciseType)
            
            // Update user history for future personalization
            userHistoryManager.updateHistory(userId, exerciseType, formScore, biomechanicalAnalysis)
            
            return AIFormFeedback(
                overallScore = formScore.overallScore,
                confidence = formScore.confidence,
                personalizedMessage = personalizedTips.primaryMessage,
                secondaryTips = personalizedTips.secondaryTips,
                biomechanicalInsights = biomechanicalAnalysis,
                adaptiveRecommendations = adaptiveFeedback,
                isCorrectForm = formScore.overallScore >= CONFIDENCE_THRESHOLD,
                improvementAreas = identifyImprovementAreas(formScore, userHistory),
                progressIndicator = calculateProgress(userHistory, formScore)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI form analysis: ${e.message}")
            return createFallbackFeedback(pose, exerciseType)
        }
    }
    
    /**
     * Advanced biomechanical analysis of the pose
     */
    private fun analyzeBiomechanics(pose: Pose, exerciseType: ExerciseType): BiomechanicalAnalysis {
        val jointAngles = calculateJointAngles(pose)
        val muscleActivation = estimateMuscleActivation(pose, exerciseType)
        val balanceAnalysis = analyzeBalance(pose)
        val rangeOfMotion = calculateRangeOfMotion(pose, exerciseType)
        
        return BiomechanicalAnalysis(
            jointAngles = jointAngles,
            muscleActivation = muscleActivation,
            balanceScore = balanceAnalysis.score,
            balanceRecommendations = balanceAnalysis.recommendations,
            rangeOfMotion = rangeOfMotion,
            injuryRiskFactors = assessInjuryRisk(jointAngles, muscleActivation)
        )
    }
    
    /**
     * Calculate comprehensive joint angles for biomechanical analysis
     */
    private fun calculateJointAngles(pose: Pose): Map<String, Float> {
        val angles = mutableMapOf<String, Float>()
        
        try {
            // Shoulder angles
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
            
            if (leftShoulder != null && leftElbow != null && leftWrist != null) {
                angles["left_shoulder"] = calculateAngle(leftShoulder, leftElbow, leftWrist)
            }
            
            // Hip angles
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
            
            if (leftHip != null && leftKnee != null && leftAnkle != null) {
                angles["left_hip"] = calculateAngle(leftHip, leftKnee, leftAnkle)
            }
            
            // Knee angles
            if (leftHip != null && leftKnee != null && leftAnkle != null) {
                angles["left_knee"] = calculateAngle(leftHip, leftKnee, leftAnkle)
            }
            
            // Spine angle
            angles["spine"] = calculateSpineAngle(pose)
            
            // Symmetry analysis
            angles["symmetry_score"] = calculateSymmetryScore(pose)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating joint angles: ${e.message}")
        }
        
        return angles
    }
    
    /**
     * Estimate muscle activation patterns based on pose
     */
    private fun estimateMuscleActivation(pose: Pose, exerciseType: ExerciseType): MuscleActivation {
        return when (exerciseType) {
            ExerciseType.SQUAT -> estimateSquatMuscleActivation(pose)
            ExerciseType.BENCH_PRESS -> estimateBenchPressMuscleActivation(pose)
            ExerciseType.DEADLIFT -> estimateDeadliftMuscleActivation(pose)
            ExerciseType.PLANK -> estimatePlankMuscleActivation(pose)
            else -> MuscleActivation.default()
        }
    }
    
    private fun estimateSquatMuscleActivation(pose: Pose): MuscleActivation {
        val kneeAngle = calculateKneeAngle(pose)
        val hipAngle = calculateHipAngle(pose)
        val spineAngle = calculateSpineAngle(pose)
        
        return MuscleActivation(
            quadriceps = calculateQuadricepsActivation(kneeAngle),
            glutes = calculateGlutesActivation(hipAngle),
            hamstrings = calculateHamstringsActivation(kneeAngle, hipAngle),
            core = calculateCoreActivation(spineAngle),
            calves = 0.3f // Base activation for squats
        )
    }
    
    private fun estimateBenchPressMuscleActivation(pose: Pose): MuscleActivation {
        val shoulderAngle = calculateShoulderAngle(pose)
        val elbowAngle = calculateElbowAngle(pose)
        
        return MuscleActivation(
            chest = calculateChestActivation(shoulderAngle, elbowAngle),
            shoulders = calculateShoulderActivation(shoulderAngle),
            triceps = calculateTricepsActivation(elbowAngle),
            core = calculateCoreActivation(calculateSpineAngle(pose))
        )
    }
    
    private fun estimateDeadliftMuscleActivation(pose: Pose): MuscleActivation {
        val hipAngle = calculateHipAngle(pose)
        val spineAngle = calculateSpineAngle(pose)
        val kneeAngle = calculateKneeAngle(pose)
        
        return MuscleActivation(
            hamstrings = calculateHamstringsActivation(kneeAngle, hipAngle),
            glutes = calculateGlutesActivation(hipAngle),
            lowerBack = calculateLowerBackActivation(spineAngle),
            core = calculateCoreActivation(spineAngle),
            traps = 0.7f // High activation in deadlifts
        )
    }
    
    private fun estimatePlankMuscleActivation(pose: Pose): MuscleActivation {
        val spineAngle = calculateSpineAngle(pose)
        val shoulderAngle = calculateShoulderAngle(pose)
        
        return MuscleActivation(
            core = max(0.8f, calculateCoreActivation(spineAngle)),
            shoulders = calculateShoulderActivation(shoulderAngle),
            chest = 0.4f,
            glutes = 0.6f
        )
    }
    
    /**
     * Analyze balance and stability
     */
    private fun analyzeBalance(pose: Pose): BalanceAnalysis {
        val centerOfMass = calculateCenterOfMass(pose)
        val baseOfSupport = calculateBaseOfSupport(pose)
        val sway = calculatePosturalSway(pose)
        
        val balanceScore = calculateBalanceScore(centerOfMass, baseOfSupport, sway)
        val recommendations = generateBalanceRecommendations(balanceScore, sway)
        
        return BalanceAnalysis(balanceScore, recommendations)
    }
    
    /**
     * Calculate range of motion for the exercise
     */
    private fun calculateRangeOfMotion(pose: Pose, exerciseType: ExerciseType): RangeOfMotion {
        return when (exerciseType) {
            ExerciseType.SQUAT -> calculateSquatROM(pose)
            ExerciseType.SHOULDER_PRESS -> calculateShoulderPressROM(pose)
            ExerciseType.DEADLIFT -> calculateDeadliftROM(pose)
            else -> RangeOfMotion.default()
        }
    }
    
    /**
     * Assess injury risk factors
     */
    private fun assessInjuryRisk(
        jointAngles: Map<String, Float>,
        muscleActivation: MuscleActivation
    ): List<InjuryRiskFactor> {
        val riskFactors = mutableListOf<InjuryRiskFactor>()
        
        // Check for dangerous joint angles
        jointAngles.forEach { (joint, angle) ->
            when (joint) {
                "left_knee", "right_knee" -> {
                    if (angle < 90f || angle > 160f) {
                        riskFactors.add(
                            InjuryRiskFactor(
                                type = "Knee Angle",
                                severity = if (angle < 70f) "High" else "Medium",
                                description = "Knee angle outside safe range",
                                recommendation = "Adjust knee position to 90-160 degrees"
                            )
                        )
                    }
                }
                "spine" -> {
                    if (angle < 160f) {
                        riskFactors.add(
                            InjuryRiskFactor(
                                type = "Spinal Alignment",
                                severity = if (angle < 140f) "High" else "Medium",
                                description = "Spine not properly aligned",
                                recommendation = "Keep your back straight and core engaged"
                            )
                        )
                    }
                }
            }
        }
        
        // Check muscle imbalances
        if (muscleActivation.hasImbalance()) {
            riskFactors.add(
                InjuryRiskFactor(
                    type = "Muscle Imbalance",
                    severity = "Medium",
                    description = "Detected muscle activation imbalance",
                    recommendation = "Focus on proper form to ensure balanced muscle engagement"
                )
            )
        }
        
        return riskFactors
    }
    
    /**
     * Generate adaptive feedback based on user progression
     */
    private fun generateAdaptiveFeedback(
        userHistory: UserFormHistory,
        formScore: FormScore
    ): List<AdaptiveRecommendation> {
        val recommendations = mutableListOf<AdaptiveRecommendation>()
        
        // Skill level adaptation
        when (userHistory.skillLevel) {
            SkillLevel.BEGINNER -> {
                recommendations.add(
                    AdaptiveRecommendation(
                        type = "Form Focus",
                        message = "Focus on basic form before adding complexity",
                        priority = "High"
                    )
                )
            }
            SkillLevel.INTERMEDIATE -> {
                if (formScore.overallScore > 0.8f) {
                    recommendations.add(
                        AdaptiveRecommendation(
                            type = "Progression",
                            message = "Great form! Consider adding more challenging variations",
                            priority = "Medium"
                        )
                    )
                }
            }
            SkillLevel.ADVANCED -> {
                recommendations.add(
                    AdaptiveRecommendation(
                        type = "Refinement",
                        message = "Focus on micro-adjustments for optimal performance",
                        priority = "Low"
                    )
                )
            }
        }
        
        // Consistency analysis
        if (userHistory.hasConsistentMistakes()) {
            recommendations.add(
                AdaptiveRecommendation(
                    type = "Pattern Correction",
                    message = "Working on correcting recurring form issues",
                    priority = "High"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Identify specific areas for improvement
     */
    private fun identifyImprovementAreas(
        formScore: FormScore,
        userHistory: UserFormHistory
    ): List<ImprovementArea> {
        val areas = mutableListOf<ImprovementArea>()
        
        formScore.componentScores.forEach { (component, score) ->
            if (score < 0.7f) {
                val historicalPerformance = userHistory.getComponentHistory(component)
                val trend = calculateTrend(historicalPerformance)
                
                areas.add(
                    ImprovementArea(
                        component = component,
                        currentScore = score,
                        targetScore = 0.8f,
                        trend = trend,
                        specificTips = generateSpecificTips(component, score),
                        estimatedImprovementTime = estimateImprovementTime(score, trend)
                    )
                )
            }
        }
        
        return areas
    }
    
    /**
     * Calculate progress indicator
     */
    private fun calculateProgress(
        userHistory: UserFormHistory,
        currentScore: FormScore
    ): ProgressIndicator {
        val recentScores = userHistory.getRecentScores(10)
        val improvement = if (recentScores.isNotEmpty()) {
            currentScore.overallScore - recentScores.first()
        } else 0f
        
        return ProgressIndicator(
            currentLevel = determineSkillLevel(currentScore.overallScore),
            improvementRate = improvement,
            consistencyScore = calculateConsistency(recentScores),
            nextMilestone = calculateNextMilestone(currentScore.overallScore),
            encouragementMessage = generateEncouragementMessage(improvement, currentScore.overallScore)
        )
    }
    
    /**
     * Create fallback feedback when AI analysis fails
     */
    private fun createFallbackFeedback(pose: Pose, exerciseType: ExerciseType): AIFormFeedback {
        return AIFormFeedback(
            overallScore = 0.5f,
            confidence = 0.3f,
            personalizedMessage = "Basic form analysis available",
            secondaryTips = listOf("Focus on proper alignment"),
            biomechanicalInsights = BiomechanicalAnalysis.default(),
            adaptiveRecommendations = emptyList(),
            isCorrectForm = false,
            improvementAreas = emptyList(),
            progressIndicator = ProgressIndicator.default()
        )
    }
    
    // Helper functions for angle calculations
    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Float {
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
    
    private fun calculateSpineAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        
        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return 0f
        }
        
        val shoulderMidpoint = Point(
            (leftShoulder.position.x + rightShoulder.position.x) / 2,
            (leftShoulder.position.y + rightShoulder.position.y) / 2
        )
        
        val hipMidpoint = Point(
            (leftHip.position.x + rightHip.position.x) / 2,
            (leftHip.position.y + rightHip.position.y) / 2
        )
        
        val deltaX = hipMidpoint.x - shoulderMidpoint.x
        val deltaY = hipMidpoint.y - shoulderMidpoint.y
        
        return Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat() + 90
    }
    
    // Additional helper methods for specific calculations
    private fun calculateKneeAngle(pose: Pose): Float {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        
        return if (leftHip != null && leftKnee != null && leftAnkle != null) {
            calculateAngle(leftHip, leftKnee, leftAnkle)
        } else 0f
    }
    
    private fun calculateHipAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        
        return if (leftShoulder != null && leftHip != null && leftKnee != null) {
            calculateAngle(leftShoulder, leftHip, leftKnee)
        } else 0f
    }
    
    private fun calculateShoulderAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        
        return if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            calculateAngle(leftShoulder, leftElbow, leftWrist)
        } else 0f
    }
    
    private fun calculateElbowAngle(pose: Pose): Float {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        
        return if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            calculateAngle(leftShoulder, leftElbow, leftWrist)
        } else 0f
    }
    
    private fun calculateSymmetryScore(pose: Pose): Float {
        // Calculate symmetry between left and right sides
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        
        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return 0f
        }
        
        val shoulderDiff = abs(leftShoulder.position.y - rightShoulder.position.y)
        val hipDiff = abs(leftHip.position.y - rightHip.position.y)
        
        val avgDiff = (shoulderDiff + hipDiff) / 2
        return max(0f, 1f - (avgDiff / 100f)) // Normalize to 0-1 scale
    }
    
    // Muscle activation calculation methods
    private fun calculateQuadricepsActivation(kneeAngle: Float): Float {
        return when {
            kneeAngle < 90f -> 0.9f // High activation in deep squat
            kneeAngle < 120f -> 0.7f
            else -> 0.4f
        }
    }
    
    private fun calculateGlutesActivation(hipAngle: Float): Float {
        return when {
            hipAngle < 90f -> 0.8f // High activation when hips are back
            hipAngle < 120f -> 0.6f
            else -> 0.3f
        }
    }
    
    private fun calculateHamstringsActivation(kneeAngle: Float, hipAngle: Float): Float {
        val kneeComponent = if (kneeAngle < 90f) 0.6f else 0.3f
        val hipComponent = if (hipAngle < 100f) 0.5f else 0.2f
        return (kneeComponent + hipComponent) / 2
    }
    
    private fun calculateCoreActivation(spineAngle: Float): Float {
        return when {
            spineAngle > 170f -> 0.8f // Good spine alignment requires core engagement
            spineAngle > 150f -> 0.6f
            else -> 0.9f // Higher activation needed to compensate for poor alignment
        }
    }
    
    private fun calculateChestActivation(shoulderAngle: Float, elbowAngle: Float): Float {
        val shoulderComponent = if (shoulderAngle in 70f..110f) 0.8f else 0.5f
        val elbowComponent = if (elbowAngle in 80f..100f) 0.7f else 0.4f
        return (shoulderComponent + elbowComponent) / 2
    }
    
    private fun calculateShoulderActivation(shoulderAngle: Float): Float {
        return when {
            shoulderAngle in 80f..100f -> 0.9f
            shoulderAngle in 60f..120f -> 0.7f
            else -> 0.5f
        }
    }
    
    private fun calculateTricepsActivation(elbowAngle: Float): Float {
        return when {
            elbowAngle < 90f -> 0.8f // High activation during pressing motion
            elbowAngle < 120f -> 0.6f
            else -> 0.3f
        }
    }
    
    private fun calculateLowerBackActivation(spineAngle: Float): Float {
        return when {
            spineAngle < 150f -> 0.9f // High activation to maintain spine position
            spineAngle < 170f -> 0.7f
            else -> 0.5f
        }
    }
    
    // Balance and stability calculations
    private fun calculateCenterOfMass(pose: Pose): Point {
        val landmarks = pose.allPoseLandmarks
        var totalX = 0f
        var totalY = 0f
        var count = 0
        
        landmarks.forEach { landmark ->
            totalX += landmark.position.x
            totalY += landmark.position.y
            count++
        }
        
        return Point(totalX / count, totalY / count)
    }
    
    private fun calculateBaseOfSupport(pose: Pose): Float {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        
        return if (leftAnkle != null && rightAnkle != null) {
            abs(rightAnkle.position.x - leftAnkle.position.x)
        } else 0f
    }
    
    private fun calculatePosturalSway(pose: Pose): Float {
        // Simplified sway calculation based on pose stability
        val centerOfMass = calculateCenterOfMass(pose)
        val baseOfSupport = calculateBaseOfSupport(pose)
        
        return if (baseOfSupport > 0) {
            abs(centerOfMass.x) / baseOfSupport
        } else 1f
    }
    
    private fun calculateBalanceScore(
        centerOfMass: Point,
        baseOfSupport: Float,
        sway: Float
    ): Float {
        val stabilityScore = if (baseOfSupport > 0) {
            max(0f, 1f - (sway * 2f))
        } else 0f
        
        return max(0f, min(1f, stabilityScore))
    }
    
    private fun generateBalanceRecommendations(
        balanceScore: Float,
        sway: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (balanceScore < 0.6f) {
            recommendations.add("Focus on widening your stance for better stability")
            recommendations.add("Engage your core muscles")
        }
        
        if (sway > 0.3f) {
            recommendations.add("Minimize excessive body movement")
            recommendations.add("Find a fixed point to focus on")
        }
        
        return recommendations
    }
    
    // Range of motion calculations
    private fun calculateSquatROM(pose: Pose): RangeOfMotion {
        val kneeAngle = calculateKneeAngle(pose)
        val hipAngle = calculateHipAngle(pose)
        
        return RangeOfMotion(
            primary = "Knee Flexion",
            currentAngle = kneeAngle,
            optimalRange = 90f..135f,
            secondaryJoints = mapOf(
                "Hip Flexion" to hipAngle
            )
        )
    }
    
    private fun calculateShoulderPressROM(pose: Pose): RangeOfMotion {
        val shoulderAngle = calculateShoulderAngle(pose)
        val elbowAngle = calculateElbowAngle(pose)
        
        return RangeOfMotion(
            primary = "Shoulder Flexion",
            currentAngle = shoulderAngle,
            optimalRange = 160f..180f,
            secondaryJoints = mapOf(
                "Elbow Extension" to elbowAngle
            )
        )
    }
    
    private fun calculateDeadliftROM(pose: Pose): RangeOfMotion {
        val hipAngle = calculateHipAngle(pose)
        val kneeAngle = calculateKneeAngle(pose)
        
        return RangeOfMotion(
            primary = "Hip Hinge",
            currentAngle = hipAngle,
            optimalRange = 70f..110f,
            secondaryJoints = mapOf(
                "Knee Flexion" to kneeAngle
            )
        )
    }
    
    // Progress and improvement methods
    private fun calculateTrend(historicalData: List<Float>): String {
        if (historicalData.size < 3) return "Insufficient Data"
        
        val recent = historicalData.takeLast(3).average()
        val older = historicalData.take(3).average()
        
        return when {
            recent > older + 0.1 -> "Improving"
            recent < older - 0.1 -> "Declining"
            else -> "Stable"
        }
    }
    
    private fun generateSpecificTips(component: String, score: Float): List<String> {
        return when (component.lowercase()) {
            "knee_alignment" -> listOf(
                "Keep knees in line with toes",
                "Avoid knee cave-in",
                "Focus on proper squat depth"
            )
            "spine_alignment" -> listOf(
                "Maintain neutral spine",
                "Engage core muscles",
                "Avoid excessive forward lean"
            )
            "shoulder_position" -> listOf(
                "Keep shoulders back and down",
                "Maintain shoulder stability",
                "Avoid shoulder elevation"
            )
            else -> listOf("Focus on proper form and alignment")
        }
    }
    
    private fun estimateImprovementTime(score: Float, trend: String): String {
        return when {
            score > 0.6f && trend == "Improving" -> "1-2 weeks"
            score > 0.4f -> "2-4 weeks"
            else -> "4-6 weeks"
        }
    }
    
    private fun determineSkillLevel(score: Float): String {
        return when {
            score >= 0.8f -> "Advanced"
            score >= 0.6f -> "Intermediate"
            else -> "Beginner"
        }
    }
    
    private fun calculateConsistency(scores: List<Float>): Float {
        if (scores.size < 3) return 0f
        
        val variance = scores.map { (it - scores.average()).pow(2) }.average()
        val stdDev = sqrt(variance).toFloat()
        
        return max(0f, 1f - (stdDev * 2f))
    }
    
    private fun calculateNextMilestone(currentScore: Float): String {
        return when {
            currentScore < 0.6f -> "Reach 60% form accuracy"
            currentScore < 0.8f -> "Achieve intermediate level (80%)"
            else -> "Master advanced techniques (95%)"
        }
    }
    
    private fun generateEncouragementMessage(improvement: Float, currentScore: Float): String {
        return when {
            improvement > 0.1f -> "Great progress! You're improving quickly!"
            improvement > 0.05f -> "Nice improvement! Keep up the good work!"
            currentScore > 0.8f -> "Excellent form! You're doing great!"
            else -> "Stay consistent and you'll see improvement!"
        }
    }
    
    data class Point(val x: Float, val y: Float)
}
