package com.grippo.data.features.api.exercise.example.models

import com.grippo.data.features.api.muscle.models.Muscle

public data class ExerciseExampleBundle(
    val id: String,
    val muscle: Muscle,
    val percentage: Int
)