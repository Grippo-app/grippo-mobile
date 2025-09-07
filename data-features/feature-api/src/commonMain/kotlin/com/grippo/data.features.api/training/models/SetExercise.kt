package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

public data class SetExercise(
    val name: String,
    val iterations: List<SetIteration>,
    val exerciseExample: ExerciseExampleValue?,
    val repetitions: Int,
    val intensity: Float,
    val volume: Float
)