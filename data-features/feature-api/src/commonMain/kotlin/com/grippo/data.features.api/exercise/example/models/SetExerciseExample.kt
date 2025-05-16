package com.grippo.data.features.api.exercise.example.models

public data class SetExerciseExample(
    val bundle: List<SetExerciseExampleBundle>,
    val tutorial: SetExerciseExampleTutorial?,
    val equipmentIds: List<String>,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val experience: ExperienceEnum,
    val forceType: ForceTypeEnum,
    val weightType: WeightTypeEnum,
    val category: CategoryEnum
)