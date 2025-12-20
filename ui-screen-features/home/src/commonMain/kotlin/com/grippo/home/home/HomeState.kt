package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.digest.MonthlyDigestState
import com.grippo.core.state.digest.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingState

@Immutable
internal data class HomeState(
    val lastTraining: TrainingState? = null,
    val weeklyDigestState: WeeklyDigestState? = null,
    val monthlyDigestState: MonthlyDigestState? = null,
)