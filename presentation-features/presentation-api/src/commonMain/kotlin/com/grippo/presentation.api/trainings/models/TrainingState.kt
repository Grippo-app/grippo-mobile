package com.grippo.presentation.api.trainings.models

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDateTime

@Immutable
public data class TrainingState(
    val id: String,
    val duration: Long,
    val createdAt: LocalDateTime,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val exercises: ImmutableList<ExerciseState>
)