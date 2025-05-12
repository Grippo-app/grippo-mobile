package com.grippo.data.features.api.user.models

import com.grippo.data.features.api.exercise.example.models.ExperienceEnumEnum

public data class User(
    val id: String,
    val name: String,
    val email: String,
    val experience: ExperienceEnumEnum,
    val weight: Float,
    val height: Int
)