package com.grippo.data.features.api.user.models

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum

public data class CreateUserProfile(
    val name: String,
    val weight: Float,
    val experience: ExperienceEnum,
    val height: Int,
    val excludeMuscleIds: List<String>,
    val excludeEquipmentIds: List<String>
)
