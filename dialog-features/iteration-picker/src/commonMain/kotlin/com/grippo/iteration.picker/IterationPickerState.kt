package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState

@Immutable
public data class IterationPickerState(
    val volume: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
)
