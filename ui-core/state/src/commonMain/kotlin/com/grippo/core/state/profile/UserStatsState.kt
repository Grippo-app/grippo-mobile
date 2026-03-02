package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import kotlin.time.Duration

@Immutable
public data class UserStatsState(
    val trainingsCount: Int,
    val totalDuration: Duration,
    val totalVolume: VolumeFormatState,
    val totalRepetitions: RepetitionsFormatState,
)
