package com.grippo.data.features.api.exercise.example.models

import com.grippo.data.features.api.equipment.models.Equipment

public data class ExerciseExample(
    val value: ExerciseExampleValue,
    val bundles: List<ExerciseExampleBundle>,
    val equipments: List<Equipment>,
    val tutorials: List<Tutorial>,
)