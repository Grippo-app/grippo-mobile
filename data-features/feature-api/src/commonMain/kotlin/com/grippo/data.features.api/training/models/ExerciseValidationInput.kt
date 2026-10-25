package com.grippo.data.features.api.training.models

import kotlinx.datetime.LocalDateTime

public data class ExerciseValidationInput(
    val name: String,
    val exampleId: String,
    val iterations: List<SetIteration>,
    val performedAt: LocalDateTime?,
)
