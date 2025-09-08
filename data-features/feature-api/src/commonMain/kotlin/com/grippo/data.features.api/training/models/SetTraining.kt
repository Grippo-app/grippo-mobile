package com.grippo.data.features.api.training.models

import kotlin.time.Duration

public data class SetTraining(
    val exercises: List<SetExercise>,
    val duration: Duration,
    val repetitions: Int,
    val intensity: Float,
    val volume: Float
)