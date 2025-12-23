package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.digest.MonthlyDigestState
import com.grippo.core.state.trainings.digest.WeeklyDigestState
import com.grippo.core.state.trainings.highlight.Highlight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HomeState(
    val lastTraining: TrainingState? = null,
    val weeklyDigestState: WeeklyDigestState? = null,
    val monthlyDigestState: MonthlyDigestState? = null,
    val highlight: Highlight? = null,
    val trainings: ImmutableList<TrainingState> = persistentListOf(),
)
