package com.grippo.data.features.api.training.models

public data class DraftTraining(
    val exercises: List<Exercise> = emptyList(),
    val duration: Long,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float
)