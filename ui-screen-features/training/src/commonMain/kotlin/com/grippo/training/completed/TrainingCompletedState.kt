package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.TrainingListValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrainingCompletedState(
    val training: ImmutableList<TrainingListValue> = persistentListOf()
)
