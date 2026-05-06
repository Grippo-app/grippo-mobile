package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

public data class PresetExercise(
    val exerciseExample: ExerciseExampleValue,
    val iterations: List<PresetIteration>,
)
