package com.grippo.data.features.api.exercise.example.models

public data class ExerciseExampleFilter(
    val query: String? = null,
    val weightType: String? = null,
    val forceType: String? = null,
    val experience: String? = null,
    val category: String? = null,
    val muscleIds: List<String> = emptyList(),
    val equipmentIds: List<String> = emptyList()
)