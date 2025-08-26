package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.TrainingState

@Immutable
internal data class TrainingCompletedState(
    val training: TrainingState? = null
)
