package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
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
import com.grippo.design.resources.provider.goal_card_status_completed
import com.grippo.design.resources.provider.goal_card_status_drifting
import com.grippo.design.resources.provider.goal_card_status_off_track
import com.grippo.design.resources.provider.goal_card_status_on_track
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class GoalProgressState(
    val goal: GoalState,
    val now: DateTimeFormatState,

    // Adherence score.
    val score: Int,

    // Pooled dimension shares (0..100).
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,

    // Timeline.
    val daysTotal: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val progressFraction: Float,
    val isFinished: Boolean,

    // Calculation artifacts.
    val sessionCount: Int,
    val compoundRatio: Int,
    val topExercises: ImmutableList<TopExerciseContributionState>,
    val topMuscles: ImmutableList<TopMuscleContributionState>,
    val topMuscleGroups: ImmutableList<TopMuscleGroupContributionState>,
) {
    /** True when there is no meaningful training data behind the score. */
    public val hasBreakdown: Boolean
        get() = sessionCount > 0 ||
                topExercises.isNotEmpty() ||
                topMuscles.isNotEmpty() ||
                topMuscleGroups.isNotEmpty()

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
        public const val CONSISTENCY_TARGET_SESSIONS: Int = 3
    }
}

private fun stubTopExercises(): ImmutableList<TopExerciseContributionState> = persistentListOf(
    TopExerciseContributionState(
        exampleId = "ex-1",
        name = "Back Squat",
        totalSets = 12,
        stimulusShare = 28,
        heaviestWeight = 140f,
        estimatedOneRepMax = 155f,
        category = CategoryEnumState.COMPOUND,
    ),
    TopExerciseContributionState(
        exampleId = "ex-2",
        name = "Bench Press",
        totalSets = 10,
        stimulusShare = 22,
        heaviestWeight = 105f,
        estimatedOneRepMax = 118f,
        category = CategoryEnumState.COMPOUND,
    ),
    TopExerciseContributionState(
        exampleId = "ex-3",
        name = "Deadlift",
        totalSets = 6,
        stimulusShare = 18,
        heaviestWeight = 180f,
        estimatedOneRepMax = 198f,
        category = CategoryEnumState.COMPOUND,
    ),
    TopExerciseContributionState(
        exampleId = "ex-4",
        name = "Barbell Row",
        totalSets = 8,
        stimulusShare = 12,
        heaviestWeight = 85f,
        estimatedOneRepMax = 96f,
        category = CategoryEnumState.COMPOUND,
    ),
    TopExerciseContributionState(
        exampleId = "ex-5",
        name = "Bicep Curl",
        totalSets = 9,
        stimulusShare = 8,
        heaviestWeight = 22f,
        estimatedOneRepMax = 26f,
        category = CategoryEnumState.ISOLATION,
    ),
)

private fun stubTopMuscles(): ImmutableList<TopMuscleContributionState> = persistentListOf(
    TopMuscleContributionState(MuscleEnumState.QUADRICEPS, 32),
    TopMuscleContributionState(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL, 22),
    TopMuscleContributionState(MuscleEnumState.LATISSIMUS_DORSI, 15),
)

private fun stubTopMuscleGroups(): ImmutableList<TopMuscleGroupContributionState> =
    persistentListOf(
        TopMuscleGroupContributionState(MuscleGroupEnumState.LEGS, 35),
        TopMuscleGroupContributionState(MuscleGroupEnumState.CHEST_MUSCLES, 22),
        TopMuscleGroupContributionState(MuscleGroupEnumState.BACK_MUSCLES, 18),
        TopMuscleGroupContributionState(MuscleGroupEnumState.SHOULDER_MUSCLES, 12),
        TopMuscleGroupContributionState(MuscleGroupEnumState.ARMS_AND_FOREARMS, 10),
        TopMuscleGroupContributionState(MuscleGroupEnumState.ABDOMINAL_MUSCLES, 3),
    )

public fun stubGoalProgress(
    primary: GoalPrimaryGoalEnumState = GoalPrimaryGoalEnumState.GET_STRONGER,
    secondary: GoalSecondaryGoalEnumState? = GoalSecondaryGoalEnumState.CONSISTENCY,
    score: Int = 75,
    strengthShare: Int = 52,
    hypertrophyShare: Int = 33,
    enduranceShare: Int = 15,
    daysTotal: Int = 90,
    daysElapsed: Int = 18,
    isFinished: Boolean = false,
    sessionCount: Int = 6,
    compoundRatio: Int = 68,
    topExercises: ImmutableList<TopExerciseContributionState> = stubTopExercises(),
    topMuscles: ImmutableList<TopMuscleContributionState> = stubTopMuscles(),
    topMuscleGroups: ImmutableList<TopMuscleGroupContributionState> = stubTopMuscleGroups(),
): GoalProgressState {
    val baseGoal = stubGoal()
    return GoalProgressState(
        goal = baseGoal.copy(primaryGoal = primary, secondaryGoal = secondary),
        now = DateTimeFormatState.of(
            value = DateTimeUtils.now(),
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        score = score,
        strengthShare = strengthShare,
        hypertrophyShare = hypertrophyShare,
        enduranceShare = enduranceShare,
        daysTotal = daysTotal.coerceAtLeast(1),
        daysElapsed = daysElapsed.coerceAtLeast(0),
        daysRemaining = (daysTotal - daysElapsed),
        progressFraction = (daysElapsed.toFloat() / daysTotal.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f),
        isFinished = isFinished,
        sessionCount = sessionCount,
        compoundRatio = compoundRatio,
        topExercises = topExercises,
        topMuscles = topMuscles,
        topMuscleGroups = topMuscleGroups,
    )
}

public fun stubGoalProgressList(): List<GoalProgressState> {
    val exercises = stubTopExercises()
    val muscles = stubTopMuscles()
    val muscleGroups = stubTopMuscleGroups()
    val seeds = listOf(
        // score, strength/hyp/end, daysTotal, daysElapsed, daysRemaining, fraction, finished
        StubSeed(82, 45, 40, 15, 90, 18, 72, 0.2f, false),
        StubSeed(54, 20, 35, 45, 60, 30, 30, 0.5f, false),
        StubSeed(28, 10, 25, 65, 45, 38, 7, 0.84f, false),
        StubSeed(66, 33, 33, 34, 30, 30, 0, 1f, false),
        StubSeed(47, 25, 50, 25, 30, 34, -4, 1f, false),
        StubSeed(91, 50, 35, 15, 120, 120, 0, 1f, true),
        StubSeed(75, 60, 25, 15, 180, 2, 178, 0.01f, false),
    )

    val now = DateTimeFormatState.of(
        value = DateTimeUtils.now(),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )

    return seeds.map { s ->
        GoalProgressState(
            goal = stubGoal(),
            now = now,
            score = s.score,
            strengthShare = s.str,
            hypertrophyShare = s.hyp,
            enduranceShare = s.end,
            daysTotal = s.daysTotal,
            daysElapsed = s.daysElapsed,
            daysRemaining = s.daysRemaining,
            progressFraction = s.fraction,
            isFinished = s.finished,
            sessionCount = 6,
            compoundRatio = 68,
            topExercises = exercises,
            topMuscles = muscles,
            topMuscleGroups = muscleGroups,
        )
    }
}

/** Internal holder used only by stub builders. */
private data class StubSeed(
    val score: Int,
    val str: Int,
    val hyp: Int,
    val end: Int,
    val daysTotal: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val fraction: Float,
    val finished: Boolean,
)
