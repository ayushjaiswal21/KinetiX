package com.asdevs.kinematix.models;

/**
 * Sample UserFormHistory and other class placeholders for AI functionality.
 */
class UserFormHistory {
    var skillLevel: SkillLevel = SkillLevel.BEGINNER
    fun getComponentHistory(component: String): List<Float> = listOf(0.5f, 0.6f, 0.7f)
    fun getRecentScores(count: Int): List<Float> = listOf(0.4f, 0.5f, 0.6f)
    fun hasConsistentMistakes(): Boolean = true
}

enum class SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class BiomechanicalAnalysis(val jointAngles: Map<String, Float>,
                                 val muscleActivation: MuscleActivation,
                                 val balanceScore: Float,
                                 val balanceRecommendations: List<String>,
                                 val rangeOfMotion: RangeOfMotion,
                                 val injuryRiskFactors: List<InjuryRiskFactor>) {
    companion object {
        fun default() = BiomechanicalAnalysis(emptyMap(), MuscleActivation.default(), 0f, emptyList(),
            RangeOfMotion.default(), emptyList())
    }
}

data class MuscleActivation(val quadriceps: Float = 0f,
                           val glutes: Float = 0f,
                           val hamstrings: Float = 0f,
                           val core: Float = 0f,
                           val calves: Float = 0f,
                           val chest: Float = 0f,
                           val shoulders: Float = 0f,
                           val triceps: Float = 0f,
                           val lowerBack: Float = 0f,
                           val traps: Float = 0f) {
    companion object {
        fun default() = MuscleActivation()
    }

    fun hasImbalance() = false
}

data class RangeOfMotion(val primary: String = "",
                         val currentAngle: Float = 0f,
                         val optimalRange: ClosedRange<Float> = 0f..0f,
                         val secondaryJoints: Map<String, Float> = emptyMap()) {
    companion object {
        fun default() = RangeOfMotion()
    }
}

data class InjuryRiskFactor(val type: String,
                            val severity: String,
                            val description: String,
                            val recommendation: String)

data class AdaptiveRecommendation(val type: String, val message: String, val priority: String)

data class ImprovementArea(val component: String,
                           val currentScore: Float,
                           val targetScore: Float,
                           val trend: String,
                           val specificTips: List<String>,
                           val estimatedImprovementTime: String)

data class ProgressIndicator(val currentLevel: String = "",
                             val improvementRate: Float = 0f,
                             val consistencyScore: Float = 0f,
                             val nextMilestone: String = "",
                             val encouragementMessage: String = "") {
    companion object {
        fun default() = ProgressIndicator()
    }
}

data class BalanceAnalysis(val score: Float, val recommendations: List<String>)
