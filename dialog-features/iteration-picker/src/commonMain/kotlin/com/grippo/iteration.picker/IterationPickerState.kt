package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable

@Immutable
public data class IterationPickerState(
    val volume: Float,
    val repetitions: Int,
)
