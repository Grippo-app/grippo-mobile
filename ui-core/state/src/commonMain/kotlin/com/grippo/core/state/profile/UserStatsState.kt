package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState

@Immutable
public data class UserStatsState(
    val trainingsCount: Int,
    val totalDuration: DurationFormatState,
    val totalVolume: VolumeFormatState,
    val totalRepetitions: RepetitionsFormatState,
)
