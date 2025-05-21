package com.grippo.presentation.api.trainings.models

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class ExerciseState(
    val id: String,
    // todo add exercise example
    val name: String,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val iterations: ImmutableList<IterationState>
)