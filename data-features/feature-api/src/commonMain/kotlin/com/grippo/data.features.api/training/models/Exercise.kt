package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExample

public data class Exercise(
    val id: String?,
    val name: String,
    val iterations: List<Iteration> = emptyList(),
    val exerciseExample: ExerciseExample?,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float
)