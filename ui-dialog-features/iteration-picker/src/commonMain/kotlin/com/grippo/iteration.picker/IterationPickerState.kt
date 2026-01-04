package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class IterationPickerState(
    val number: Int,
    val value: IterationState,
    val suggestions: ImmutableList<IterationState>,
    val focus: IterationFocusState,
)