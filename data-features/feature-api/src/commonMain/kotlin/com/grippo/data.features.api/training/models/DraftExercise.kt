package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import kotlinx.datetime.LocalDateTime

public data class DraftExercise(
    val name: String,
    val iterations: List<SetIteration>,
    val exerciseExample: ExerciseExampleValue,
    val createdAt: LocalDateTime,
)
