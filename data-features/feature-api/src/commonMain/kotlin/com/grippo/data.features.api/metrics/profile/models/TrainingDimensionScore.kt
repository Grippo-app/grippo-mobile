package com.grippo.data.features.api.metrics.profile.models

public data class TrainingDimensionScore(
    val kind: TrainingDimensionKind,
    val score: Int, // 0..100
)