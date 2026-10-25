package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents

public data class ExerciseValidationContext(
    val components: ExerciseExampleComponents? = null,
    val history: List<Exercise> = emptyList(),
    val excludedExerciseId: String? = null,
    val bodyWeight: Float? = null,
)
