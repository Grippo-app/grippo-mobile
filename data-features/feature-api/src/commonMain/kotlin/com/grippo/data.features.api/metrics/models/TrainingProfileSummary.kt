package com.grippo.data.features.api.metrics.models

public data class TrainingLoadProfile(
    val kind: TrainingProfileKind,
    val dimensions: List<TrainingDimensionScore>,
    val dominant: TrainingDimensionKind?,
    val confidence: Int,
)

public enum class TrainingDimensionKind {
    Strength,
    Hypertrophy,
    Endurance,
}

public enum class TrainingProfileKind {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy,
}

public data class TrainingDimensionScore(
    val kind: TrainingDimensionKind,
    val score: Int, // 0..100
)
