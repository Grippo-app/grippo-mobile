package com.grippo.data.features.api.training.models

public data class SetExercise(
    val name: String,
    val iterations: List<SetIteration>,
    val exerciseExampleId: String?,
)