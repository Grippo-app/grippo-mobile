package com.grippo.presentation.api.exercise.example.models

import com.grippo.presentation.api.user.models.ExperienceEnumState

public data class ExerciseExampleValueState(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val description: String,
    val experience: ExperienceEnumState,
    val forceType: ForceTypeEnumState,
    val weightType: WeightTypeEnumState,
    val category: CategoryEnumState,
)