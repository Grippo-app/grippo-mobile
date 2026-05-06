package com.grippo.data.features.api.training.models

import kotlin.time.Duration

public data class DraftTraining(
    val trainingId: String?,
    val duration: Duration,
    val exercises: List<DraftExercise>,
)
