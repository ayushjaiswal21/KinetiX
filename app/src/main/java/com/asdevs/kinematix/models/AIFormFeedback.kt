package com.asdevs.kinematix.models

/**
 * Data class representing feedback from the AI form analysis
 */
data class AIFormFeedback(
    val overallScore: Float,
    val confidence: Float,
    val personalizedMessage: String,
    val secondaryTips: List<String>,
    val biomechanicalInsights: BiomechanicalAnalysis,
    val adaptiveRecommendations: List<AdaptiveRecommendation>,
    val isCorrectForm: Boolean,
    val improvementAreas: List<ImprovementArea>,
    val progressIndicator: ProgressIndicator
)
