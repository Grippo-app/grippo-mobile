package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.performance.ExerciseSpotlightState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.home_unlock_duration_trend_description
import com.grippo.design.resources.provider.home_unlock_duration_trend_title
import com.grippo.design.resources.provider.home_unlock_performance_trends_description
import com.grippo.design.resources.provider.home_unlock_performance_trends_title
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
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
    val goalProgress: GoalProgressState? = null,
    val spotlights: ImmutableList<ExerciseSpotlightState> = persistentListOf(),
    val excludedMusclesCount: Int = 0,
    val missingEquipmentCount: Int = 0,
    val hasGoal: Boolean = false,
    val showWelcomeConfetti: Boolean = false,
)

@Immutable
internal enum class HomeUnlock(
    val requiredLifetimeCount: Int,
    val titleRes: StringResource,
    val descriptionRes: StringResource,
) {
    DurationTrend(
        requiredLifetimeCount = 2,
        titleRes = Res.string.home_unlock_duration_trend_title,
        descriptionRes = Res.string.home_unlock_duration_trend_description,
    ),

    PerformanceTrends(
        requiredLifetimeCount = 3,
        titleRes = Res.string.home_unlock_performance_trends_title,
        descriptionRes = Res.string.home_unlock_performance_trends_description,
    );

    fun isUnlocked(lifetimeCount: Int): Boolean =
        lifetimeCount >= requiredLifetimeCount

    companion object {
        // The user is "new" until they pass the last milestone. Derived from
        // entries so adding a milestone automatically extends the new-user phase.
        val NEW_USER_THRESHOLD: Int = entries.maxOf { it.requiredLifetimeCount }

        fun nextMilestone(lifetimeCount: Int): HomeUnlock? =
            entries.firstOrNull { !it.isUnlocked(lifetimeCount) }

        fun shouldShowBanner(lifetimeCount: Int, hasGoal: Boolean): Boolean =
            lifetimeCount < NEW_USER_THRESHOLD || !hasGoal
    }
}