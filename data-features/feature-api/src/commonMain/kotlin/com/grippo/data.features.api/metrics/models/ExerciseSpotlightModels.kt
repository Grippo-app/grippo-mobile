package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExample

public data class ExerciseSpotlight(
    val example: ExerciseExample,
    val totalVolume: Float,
    val sessionCount: Int,
)
