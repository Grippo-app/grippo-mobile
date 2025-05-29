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
        name = "Dumbbell Bench press",
        imageUrl = null,
        description = "The dumbbell bench press is a variation of the barbell bench press and an exercise used to build the muscles of the chest. It is recommended after reaching a certain point of strength on the barbell bench press to avoid pec and shoulder injuries. The exercise requires maintaining shoulder stability throughout, making it a great test of strength and control.",
        experience = ExperienceEnumState.INTERMEDIATE,
        forceType = ForceTypeEnumState.PUSH,
        weightType = WeightTypeEnumState.BODY_WEIGHT,
        category = CategoryEnumState.COMPOUND
    )
}