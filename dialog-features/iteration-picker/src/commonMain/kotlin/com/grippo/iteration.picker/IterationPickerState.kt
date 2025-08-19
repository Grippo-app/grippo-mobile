package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.IterationState

@Immutable
public data class IterationPickerState(
    val value: IterationState,
    val focus: IterationFocus,
)
