package com.grippo.data.features.api.muscle.models

public data class MuscleGroup(
    val id: String,
    val name: String,
    val muscles: List<Muscle>,
    val type: MuscleGroupEnum
)