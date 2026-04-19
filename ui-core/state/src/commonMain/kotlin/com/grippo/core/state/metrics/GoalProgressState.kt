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
import com.grippo.design.resources.provider.goal_card_status_completed
import com.grippo.design.resources.provider.goal_card_status_drifting
import com.grippo.design.resources.provider.goal_card_status_off_track
import com.grippo.design.resources.provider.goal_card_status_on_track
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Immutable
public data class GoalProgressState(
    val goal: GoalState,
    val now: DateFormatState,

    // Adherence
    val score: Int,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,

    // Calculated
    val daysTotal: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val progressFraction: Float,
    val isFinished: Boolean,
) {
    @Composable
    public fun progressLine(): String {
        val day = (daysElapsed + 1).coerceAtMost(daysTotal)
        return AppTokens.strings.res(Res.string.goal_card_progress_day, day, daysTotal)
    }

    @Composable
    public fun remainingLine(): String {
        return when {
            daysRemaining > 0 -> AppTokens.strings.res(Res.string.goal_card_progress_days_left, daysRemaining)
            daysRemaining == 0 -> AppTokens.strings.res(Res.string.goal_card_progress_due_today)
            else -> AppTokens.strings.res(Res.string.goal_card_progress_overdue, -daysRemaining)
        }
    }

    @Composable
    public fun statusLabel(): String {
        val score = score.coerceIn(0, 100)
        return when {
            isFinished && score >= ON_TRACK_MIN -> AppTokens.strings.res(Res.string.goal_card_status_completed)
            score >= ON_TRACK_MIN -> AppTokens.strings.res(Res.string.goal_card_status_on_track)
            score >= DRIFTING_MIN -> AppTokens.strings.res(Res.string.goal_card_status_drifting)
            else -> AppTokens.strings.res(Res.string.goal_card_status_off_track)
        }
    }

    public companion object {
        public const val ON_TRACK_MIN: Int = 70
        public const val DRIFTING_MIN: Int = 40
    }
}

public fun stubGoalProgress(): GoalProgressState {
    val goal = stubGoal()
    val now = DateFormatState.of(
        value = DateTimeUtils.now(),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )
    return GoalProgressState(
        goal = goal,
        now = now,
        score = 72,
        strengthShare = 30,
        hypertrophyShare = 55,
        enduranceShare = 15,
        daysTotal = 1,
        daysElapsed = 0,
        daysRemaining = 0,
        progressFraction = 0f,
        isFinished = true,
    )
}
