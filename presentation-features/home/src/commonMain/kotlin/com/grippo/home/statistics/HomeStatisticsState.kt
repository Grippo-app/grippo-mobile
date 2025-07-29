package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.state.datetime.PeriodState

@Immutable
internal data class HomeStatisticsState(
    val period: PeriodState = PeriodState.DAILY
)