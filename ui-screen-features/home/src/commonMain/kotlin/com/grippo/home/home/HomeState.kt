package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.Highlight
import com.grippo.core.state.metrics.digest.MonthlyDigestState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingState

@Immutable
internal data class HomeState(
    val lastTraining: TrainingState? = null,
    val weeklyDigestState: WeeklyDigestState? = null,
    val monthlyDigestState: MonthlyDigestState? = null,
    val highlight: Highlight? = null,
)
