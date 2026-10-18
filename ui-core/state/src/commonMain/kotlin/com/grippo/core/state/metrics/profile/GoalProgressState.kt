package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.GoalState
import com.grippo.core.state.profile.stubGoal
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_progress_day
import com.grippo.design.resources.provider.goal_card_progress_days_left
import com.grippo.design.resources.provider.goal_card_progress_due_today
import com.grippo.design.resources.provider.goal_card_progress_overdue
import com.grippo.design.resources.provider.goal_card_status_not_evaluated
import com.grippo.design.resources.provider.goal_card_status_drifting
import com.grippo.design.resources.provider.goal_card_status_off_track
import com.grippo.design.resources.provider.goal_card_status_on_track
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class GoalProgressState(
    val goal: GoalState,
    val now: DateTimeFormatState,

    // Training-pattern heuristic; null when there is insufficient evidence.
    val score: Int?,

    // Descriptive shares of recorded sets by repetition range (0..100).
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,

    // Calculation artifacts.
    val sessionCount: Int,
    val findings: ImmutableList<GoalFitFindingState>,
    val evidence: GoalAnalysisEvidenceState,
) {
    public val daysTotal: Int
        get() {
            val start = goal.createdAt.value?.date ?: return 1
            val target = goal.target.value?.date ?: return 1
            return (start.daysUntil(target) + 1).coerceAtLeast(1)
        }

    public val daysElapsed: Int
        get() {
            val start = goal.createdAt.value?.date ?: return 0
            val date = now.value?.date ?: return 0
            return start.daysUntil(date).coerceIn(0, daysTotal - 1)
        }

    public val daysRemaining: Int
        get() {
            val date = now.value?.date ?: return 0
            val target = goal.target.value?.date ?: return 0
            return date.daysUntil(target)
        }

    public val progressFraction: Float
        get() = if (isFinished) {
            1f
        } else {
            (daysElapsed.toFloat() / (daysTotal - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }

    public val isFinished: Boolean
        get() {
            val date = now.value?.date ?: return false
            val target = goal.target.value?.date ?: return false
            return date >= target
        }

    @Composable
    public fun progressLine(): String {
        val day = (daysElapsed + 1).coerceAtMost(daysTotal)
        return AppTokens.strings.res(Res.string.goal_card_progress_day, day, daysTotal)
    }

    @Composable
    public fun remainingLine(): String {
        return when {
            daysRemaining > 0 -> AppTokens.strings.res(
                Res.string.goal_card_progress_days_left,
                daysRemaining
            )

            daysRemaining == 0 -> AppTokens.strings.res(Res.string.goal_card_progress_due_today)
            else -> AppTokens.strings.res(Res.string.goal_card_progress_overdue, -daysRemaining)
        }
    }

    @Composable
    public fun statusLabel(): String {
        val score = score ?: return AppTokens.strings.res(Res.string.goal_card_status_not_evaluated)
        return when {
            score >= ON_TRACK_MIN -> AppTokens.strings.res(Res.string.goal_card_status_on_track)
            score >= DRIFTING_MIN -> AppTokens.strings.res(Res.string.goal_card_status_drifting)
            else -> AppTokens.strings.res(Res.string.goal_card_status_off_track)
        }
    }

    @Composable
    public fun statusColor(): Color {
        val score = score ?: return AppTokens.colors.text.secondary
        return when {
            score >= ON_TRACK_MIN -> AppTokens.colors.semantic.success
            score >= DRIFTING_MIN -> AppTokens.colors.semantic.warning
            else -> AppTokens.colors.semantic.error
        }
    }

    public companion object {
        public const val ON_TRACK_MIN: Int = 70
        public const val DRIFTING_MIN: Int = 40
    }
}

public fun stubGoalProgress(
    primary: GoalPrimaryGoalEnumState = GoalPrimaryGoalEnumState.GET_STRONGER,
    secondary: GoalSecondaryGoalEnumState? = GoalSecondaryGoalEnumState.CONSISTENCY,
    score: Int? = 75,
    strengthShare: Int = 52,
    hypertrophyShare: Int = 33,
    enduranceShare: Int = 15,
    daysTotal: Int = 90,
    daysElapsed: Int = 18,
    sessionCount: Int = 6,
    findings: ImmutableList<GoalFitFindingState> = stubGoalFitFindings(primary),
): GoalProgressState {
    val now = LocalDateTime(2026, 10, 18, 12, 0)
    val start = now.date.minus(DatePeriod(days = daysElapsed.coerceAtLeast(0)))
    val target = start.plus(DatePeriod(days = daysTotal.coerceAtLeast(1) - 1))
    val baseGoal = stubGoal()
    return GoalProgressState(
        goal = baseGoal.copy(
            primaryGoal = primary,
            secondaryGoal = secondary,
            createdAt = DateTimeFormatState.of(
                value = start.atTime(now.time),
                range = DateTimeUtils.infinity(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            target = DateTimeFormatState.of(
                value = target.atTime(now.time),
                range = DateTimeUtils.infinity(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
        ),
        now = DateTimeFormatState.of(
            value = now,
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        score = score,
        strengthShare = strengthShare,
        hypertrophyShare = hypertrophyShare,
        enduranceShare = enduranceShare,
        sessionCount = sessionCount,
        findings = findings,
        evidence = GoalAnalysisEvidenceState(
            range = DateRangeFormatState.of(
                from = DateTimeUtils.startOfDay(now.date.minus(DatePeriod(days = 29))),
                to = DateTimeUtils.endOfDay(now.date),
            ),
            setCount = sessionCount * 12,
            excludedSetCount = 0,
            missingMetadataSetCount = 0,
        ),
    )
}

public fun stubGoalProgressList(): ImmutableList<GoalProgressState> = persistentListOf(
    stubGoalProgress(score = 82, strengthShare = 45, hypertrophyShare = 40, enduranceShare = 15),
    stubGoalProgress(score = 54, strengthShare = 20, hypertrophyShare = 35, enduranceShare = 45,
        daysTotal = 60, daysElapsed = 30),
    stubGoalProgress(score = 28, strengthShare = 10, hypertrophyShare = 25, enduranceShare = 65,
        daysTotal = 45, daysElapsed = 38),
    stubGoalProgress(score = 66, strengthShare = 33, hypertrophyShare = 33, enduranceShare = 34,
        daysTotal = 30, daysElapsed = 29),
    stubGoalProgress(score = 47, strengthShare = 25, hypertrophyShare = 50, enduranceShare = 25,
        daysTotal = 30, daysElapsed = 34),
    stubGoalProgress(score = 91, strengthShare = 50, hypertrophyShare = 35, enduranceShare = 15,
        daysTotal = 120, daysElapsed = 120),
    stubGoalProgress(score = 75, strengthShare = 60, hypertrophyShare = 25, enduranceShare = 15,
        daysTotal = 180, daysElapsed = 2),
)
