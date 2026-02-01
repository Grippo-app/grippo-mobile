package com.grippo.data.features.api.exercise.example.models

import kotlinx.datetime.LocalDateTime

public data class ExerciseExampleValue(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val experience: ExperienceEnum,
    val forceType: ForceTypeEnum,
    val weightType: WeightTypeEnum,
    val category: CategoryEnum,
    val usageCount: Int,
    val lastUsed: LocalDateTime?,
)
