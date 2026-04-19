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

public fun stubGoalProgressList(): List<GoalProgressState> {
    val now = DateFormatState.of(
        value = DateTimeUtils.now(),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )
    return listOf(
        // On track, early progress
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 82,
            strengthShare = 45,
            hypertrophyShare = 40,
            enduranceShare = 15,
            daysTotal = 90,
            daysElapsed = 18,
            daysRemaining = 72,
            progressFraction = 0.2f,
            isFinished = false,
        ),
        // Drifting, mid progress
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 54,
            strengthShare = 20,
            hypertrophyShare = 35,
            enduranceShare = 45,
            daysTotal = 60,
            daysElapsed = 30,
            daysRemaining = 30,
            progressFraction = 0.5f,
            isFinished = false,
        ),
        // Off track, late progress
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 28,
            strengthShare = 10,
            hypertrophyShare = 25,
            enduranceShare = 65,
            daysTotal = 45,
            daysElapsed = 38,
            daysRemaining = 7,
            progressFraction = 0.84f,
            isFinished = false,
        ),
        // Due today
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 66,
            strengthShare = 33,
            hypertrophyShare = 33,
            enduranceShare = 34,
            daysTotal = 30,
            daysElapsed = 30,
            daysRemaining = 0,
            progressFraction = 1f,
            isFinished = false,
        ),
        // Overdue
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 47,
            strengthShare = 25,
            hypertrophyShare = 50,
            enduranceShare = 25,
            daysTotal = 30,
            daysElapsed = 34,
            daysRemaining = -4,
            progressFraction = 1f,
            isFinished = false,
        ),
        // Completed successfully
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 91,
            strengthShare = 50,
            hypertrophyShare = 35,
            enduranceShare = 15,
            daysTotal = 120,
            daysElapsed = 120,
            daysRemaining = 0,
            progressFraction = 1f,
            isFinished = true,
        ),
        // Just started
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = 75,
            strengthShare = 60,
            hypertrophyShare = 25,
            enduranceShare = 15,
            daysTotal = 180,
            daysElapsed = 2,
            daysRemaining = 178,
            progressFraction = 0.01f,
            isFinished = false,
        ),
    )
}
