package com.grippo.core.state.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.profile.GoalState
import com.grippo.core.state.profile.stubGoal
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_progress_day
import com.grippo.design.resources.provider.goal_card_progress_days_left
import com.grippo.design.resources.provider.goal_card_progress_due_today
import com.grippo.design.resources.provider.goal_card_progress_overdue
import com.grippo.design.resources.provider.goal_card_started_label
import com.grippo.design.resources.provider.goal_card_status_completed
import com.grippo.design.resources.provider.goal_card_status_drifting
import com.grippo.design.resources.provider.goal_card_status_off_track
import com.grippo.design.resources.provider.goal_card_status_on_track
import com.grippo.design.resources.provider.goal_card_target_label
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

@Immutable
public data class GoalAdherenceState(
    val score: Int,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,
)

@Immutable
public data class GoalProgressState(
    val goal: GoalState,
    val now: DateFormatState,
    val adherence: GoalAdherenceState,
) {

    private val startDate: LocalDate? get() = goal.createdAt.value?.date
    private val targetDate: LocalDate? get() = goal.target.value?.date
    private val currentDate: LocalDate? get() = now.value?.date

    /** Total span of the goal in days. Always ≥ 1 to avoid division by zero. */
    public val daysTotal: Int
        get() = startDate.daysUntil(targetDate).coerceAtLeast(1)

    /** Days that have already passed. Clamped into [0, daysTotal]. */
    public val daysElapsed: Int
        get() = startDate.daysUntil(currentDate).coerceIn(0, daysTotal)

    /** Negative if the target is already in the past (overdue). */
    public val daysRemaining: Int?
        get() = currentDate?.daysUntil(targetDate)

    /** 0.0 at start, 1.0 at or past target. */
    public val progressFraction: Float
        get() = (daysElapsed.toFloat() / daysTotal.toFloat()).coerceIn(0f, 1f)

    public val isFinished: Boolean
        get() = currentDate >= targetDate

    public val startedAtFormatted: DateFormatState
        get() = DateFormatState.of(
            value = goal.createdAt.value,
            range = DateRange.Range.Infinity().range,
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        )

    public val targetAtFormatted: DateFormatState
        get() = DateFormatState.of(
            value = goal.createdAt.value,
            range = DateRange.Range.Infinity().range,
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        )

    @Composable
    public fun progressLine(): String {
        // "Day 1 of N" on the start day, "Day N of N" on the target day.
        val day = (daysElapsed + 1).coerceAtMost(daysTotal)
        return AppTokens.strings.res(Res.string.goal_card_progress_day, day, daysTotal)
    }

    @Composable
    public fun remainingLine(): String {
        val remaining = daysRemaining
        return when {
            remaining > 0 -> AppTokens.strings.res(
                Res.string.goal_card_progress_days_left,
                remaining
            )

            remaining == 0 -> AppTokens.strings.res(Res.string.goal_card_progress_due_today)
            else -> AppTokens.strings.res(Res.string.goal_card_progress_overdue, -remaining)
        }
    }

    @Composable
    public fun statusLabel(): String {
        val score = adherence.score.coerceIn(0, 100)
        return when {
            isFinished && score >= ON_TRACK_MIN ->
                AppTokens.strings.res(Res.string.goal_card_status_completed)

            score >= ON_TRACK_MIN ->
                AppTokens.strings.res(Res.string.goal_card_status_on_track)

            score >= DRIFTING_MIN ->
                AppTokens.strings.res(Res.string.goal_card_status_drifting)

            else ->
                AppTokens.strings.res(Res.string.goal_card_status_off_track)
        }
    }

    @Composable
    public fun startedLabel(): String {
        return AppTokens.strings.res(
            Res.string.goal_card_started_label,
            startedAtFormatted.display,
        )
    }

    @Composable
    public fun targetLabel(): String {
        return AppTokens.strings.res(
            Res.string.goal_card_target_label,
            targetAtFormatted.display,
        )
    }

    public companion object {
        public const val ON_TRACK_MIN: Int = 70
        public const val DRIFTING_MIN: Int = 40
    }
}

public fun stubGoalProgress(): GoalProgressState {
    return GoalProgressState(
        goal = stubGoal(),
        now = DateFormatState.of(
            value = DateTimeUtils.now(),
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        adherence = GoalAdherenceState(
            score = 72,
            strengthShare = 30,
            hypertrophyShare = 55,
            enduranceShare = 15,
        ),
    )
}
