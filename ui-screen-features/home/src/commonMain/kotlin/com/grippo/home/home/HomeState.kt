package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.HighlightState
import com.grippo.core.state.metrics.digest.MonthlyDigestState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingState

@Immutable
internal data class HomeState(
    val lastTraining: TrainingState? = null,
    val weeklyDigest: WeeklyDigestState? = null,
    val monthlyDigest: MonthlyDigestState? = null,
    val highlight: HighlightState? = null,
)
