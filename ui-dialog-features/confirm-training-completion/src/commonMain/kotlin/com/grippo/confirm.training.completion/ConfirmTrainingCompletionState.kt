package com.grippo.confirm.training.completion

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class ConfirmTrainingCompletionState(
    val duration: DurationFormatState,
    val exercises: ImmutableList<ExerciseState>,
)
