package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.performance.ExerciseSpotlightState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

@Immutable
internal data class HomeState(
    val range: DateRangeFormatState = DateRangeFormatState.of(DateRangeKind.Last30Days),
    val user: UserState? = null,
    val hasDraftTraining: Boolean = false,
    val lastTraining: TrainingState? = null,
    val totalDuration: Duration? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val streak: TrainingStreakState? = null,
    val performance: ImmutableList<PerformanceMetricState> = persistentListOf(),
    val profile: TrainingLoadProfileState? = null,
    val goalProgress: GoalProgressState? = null,
    val spotlights: ImmutableList<ExerciseSpotlightState> = persistentListOf(),
)
