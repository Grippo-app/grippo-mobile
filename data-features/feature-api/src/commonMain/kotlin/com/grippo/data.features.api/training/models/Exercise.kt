package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import kotlinx.datetime.LocalDateTime

public data class Exercise(
    val id: String,
    val name: String,
    val iterations: List<Iteration>,
    val exerciseExample: ExerciseExampleValue,
    val createdAt: LocalDateTime,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
)