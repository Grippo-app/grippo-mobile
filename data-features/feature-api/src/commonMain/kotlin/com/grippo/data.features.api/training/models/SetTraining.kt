package com.grippo.data.features.api.training.models

public data class SetTraining(
    val exercises: List<SetExercise>,
    val duration: Long,
    val repetitions: Int,
    val intensity: Float,
    val volume: Float
)