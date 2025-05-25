package com.grippo.data.features.api.exercise.example.models

import com.grippo.data.features.api.equipment.models.Equipment

public data class ExerciseExample(
    val id: String,
    val bundles: List<ExerciseExampleBundle>,
    val equipments: List<Equipment>,
    val tutorials: List<Tutorial>,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val experience: ExperienceEnum,
    val forceType: ForceTypeEnum,
    val weightType: WeightTypeEnum,
    val category: CategoryEnum
)