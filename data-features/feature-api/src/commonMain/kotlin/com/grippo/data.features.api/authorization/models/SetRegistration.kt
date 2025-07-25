package com.grippo.data.features.api.authorization.models

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum

public data class SetRegistration(
    val email: String,
    val password: String,
    val name: String,
    val weight: Float,
    val experience: ExperienceEnum,
    val height: Int,
    val excludeMuscleIds: List<String>,
    val excludeEquipmentIds: List<String>
)