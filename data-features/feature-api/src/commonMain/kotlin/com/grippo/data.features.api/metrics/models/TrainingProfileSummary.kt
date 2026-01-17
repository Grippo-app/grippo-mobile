package com.grippo.data.features.api.metrics.models

import kotlinx.datetime.LocalDateTime

public data class TrainingProfileSummary(
    val snapshots: List<TrainingProfileSnapshot>,
)

public enum class TrainingDimensionKind {
    Strength,
    Hypertrophy,
    Endurance,
    Compoundness,
    FreeWeightBias,
    PushDominance,
    Specialization,
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

public data class TrainingProfileSnapshot(
    val trainingId: String,
    val createdAt: LocalDateTime,
    val kind: TrainingProfileKind,
    val dimensions: List<TrainingDimensionScore>,
)