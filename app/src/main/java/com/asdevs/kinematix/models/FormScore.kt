package com.asdevs.kinematix.models

/**
 * Data class representing the scoring results from form analysis
 */
data class FormScore(
    val overallScore: Float,
    val confidence: Float,
    val componentScores: Map<String, Float>
)
