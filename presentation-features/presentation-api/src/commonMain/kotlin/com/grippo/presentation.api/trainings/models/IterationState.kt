package com.grippo.presentation.api.trainings.models

import androidx.compose.runtime.Immutable

@Immutable
public data class IterationState(
    val id: String,
    val weight: Float,
    val repetitions: Int,
)