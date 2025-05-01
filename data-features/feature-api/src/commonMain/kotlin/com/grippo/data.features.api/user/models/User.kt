package com.grippo.data.features.api.user.models

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum

public data class User(
    val id: String,
    val name: String,
    val email: String,
    val experience: ExperienceEnum,
    val weight: Float,
    val height: Float
)