package com.grippo.presentation.api.exercise.example.models

import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlin.uuid.Uuid

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

public fun stubExerciseExampleValueState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = Uuid.random().toString(),
        name = "Bench press",
        imageUrl = null,
        description = "Nothing to say",
        experience = ExperienceEnumState.INTERMEDIATE,
        forceType = ForceTypeEnumState.PUSH,
        weightType = WeightTypeEnumState.BODY_WEIGHT,
        category = CategoryEnumState.COMPOUND
    )
}