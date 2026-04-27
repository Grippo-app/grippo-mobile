package com.grippo.training.goal.details

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.profile.toState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.InsightItem
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.ReasonCode
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.TipCode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class TrainingGoalDetailsViewModel(
    range: DateRange,
    trainingFeature: TrainingFeature,
    private val goalFollowingUseCase: GoalFollowingUseCase,
) : BaseViewModel<TrainingGoalDetailsDialogState, TrainingGoalDetailsDirection, TrainingGoalDetailsLoader>(
    TrainingGoalDetailsDialogState(
        range = DateRangeFormatState.of(range),
    )
), TrainingGoalDetailsContract {

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideProgress)
            .safeLaunch()
    }

    private suspend fun provideProgress(trainings: List<Training>) {
        val adherence = goalFollowingUseCase.fromTrainingsByPrimary(trainings)

        if (adherence == null) {
            update {
                it.copy(
                    progress = null,
                    insights = persistentListOf(
                        InsightItem(
                            InsightItem.Severity.Neutral,
                            ReasonCode.NoData
                        )
                    ),
                    tips = persistentListOf(),
                )
            }
            return
        }

        val progress = adherence.toState()

        val insights = buildInsights(progress)

        val tips = buildTips(progress)

        update {
            it.copy(
                progress = progress,
                insights = insights,
                tips = tips,
            )
        }
    }

    private fun buildInsights(progress: GoalProgressState): ImmutableList<InsightItem> {
        val items = mutableListOf<InsightItem>()
        val score = progress.score.coerceIn(0, 100)
        val isFinished = progress.isFinished

        // 1) Score-level summary header
        items += when {
            isFinished && score >= GoalProgressState.ON_TRACK_MIN ->
                InsightItem(InsightItem.Severity.Positive, ReasonCode.Completed)

            score >= GoalProgressState.ON_TRACK_MIN ->
                InsightItem(InsightItem.Severity.Positive, ReasonCode.ScoreOnTrack)

            score >= GoalProgressState.DRIFTING_MIN ->
                InsightItem(InsightItem.Severity.Warning, ReasonCode.ScoreDrifting)

            else ->
                InsightItem(InsightItem.Severity.Negative, ReasonCode.ScoreOffTrack)
        }

        // 2) Goal-specific focus analysis
        when (progress.goal.primaryGoal) {
            GoalPrimaryGoalEnumState.GET_STRONGER -> items += focusAxis(
                share = progress.strengthShare,
                lowReason = ReasonCode.StrengthShareLow,
                okReason = ReasonCode.StrengthShareOk,
            )

            GoalPrimaryGoalEnumState.BUILD_MUSCLE -> items += focusAxis(
                share = progress.hypertrophyShare,
                lowReason = ReasonCode.HypertrophyShareLow,
                okReason = ReasonCode.HypertrophyShareOk,
            )

            GoalPrimaryGoalEnumState.LOSE_FAT -> items += focusAxis(
                share = progress.enduranceShare,
                lowReason = ReasonCode.EnduranceShareLow,
                okReason = ReasonCode.EnduranceShareOk,
            )

            GoalPrimaryGoalEnumState.MAINTAIN -> {
                items += balanceAxis(
                    a = progress.strengthShare,
                    b = progress.hypertrophyShare,
                )
                if (progress.enduranceShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) {
                    items += InsightItem(
                        InsightItem.Severity.Warning,
                        ReasonCode.SuppressedAxisTooHigh,
                    )
                }
            }

            GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
                items += balanceAxis(
                    a = progress.strengthShare,
                    b = progress.enduranceShare,
                )
                if (progress.hypertrophyShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) {
                    items += InsightItem(
                        InsightItem.Severity.Warning,
                        ReasonCode.SuppressedAxisTooHigh,
                    )
                }
            }

            GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> items += when {
                score < GoalProgressState.DRIFTING_MIN && progress.daysElapsed >= GoalAdherence.EARLY_DAYS ->
                    InsightItem(
                        InsightItem.Severity.Warning,
                        ReasonCode.ReturnToTrainingUnderWork,
                    )

                score < GoalProgressState.ON_TRACK_MIN ->
                    InsightItem(
                        InsightItem.Severity.Warning,
                        ReasonCode.ReturnToTrainingOverWork,
                    )

                else ->
                    InsightItem(
                        InsightItem.Severity.Positive,
                        ReasonCode.ReturnToTrainingInRange,
                    )
            }
        }

        // 3) Timeline notes
        when {
            isFinished -> {
                // already covered above
            }

            progress.daysRemaining < 0 ->
                items += InsightItem(InsightItem.Severity.Warning, ReasonCode.Overdue)

            progress.daysElapsed < GoalAdherence.EARLY_DAYS ->
                items += InsightItem(InsightItem.Severity.Neutral, ReasonCode.TooEarly)

            progress.progressFraction >= GoalAdherence.ALMOST_DONE_FRACTION ->
                items += InsightItem(InsightItem.Severity.Neutral, ReasonCode.AlmostDone)
        }

        return items.toPersistentList()
    }

    private fun focusAxis(
        share: Int,
        lowReason: ReasonCode,
        okReason: ReasonCode,
    ): InsightItem {
        return when {
            share >= GoalAdherence.FOCUS_SHARE_STRONG ->
                InsightItem(InsightItem.Severity.Positive, okReason)

            share >= GoalAdherence.FOCUS_SHARE_TARGET ->
                InsightItem(InsightItem.Severity.Neutral, okReason)

            else ->
                InsightItem(InsightItem.Severity.Negative, lowReason)
        }
    }

    private fun balanceAxis(a: Int, b: Int): InsightItem {
        val delta = kotlin.math.abs(a - b)
        return if (delta <= GoalAdherence.BALANCE_DELTA_LIMIT) {
            InsightItem(InsightItem.Severity.Positive, ReasonCode.BalanceOk)
        } else {
            InsightItem(InsightItem.Severity.Warning, ReasonCode.BalanceDrift)
        }
    }

    private fun buildTips(progress: GoalProgressState): ImmutableList<TipCode> {
        val tips = mutableListOf<TipCode>()
        val score = progress.score.coerceIn(0, 100)

        // If the user is already doing great, we only leave a "stay the course" tip.
        if (progress.isFinished && score >= GoalProgressState.ON_TRACK_MIN) {
            return persistentListOf(TipCode.StayTheCourse)
        }

        when (progress.goal.primaryGoal) {
            GoalPrimaryGoalEnumState.GET_STRONGER -> {
                if (progress.strengthShare < GoalAdherence.FOCUS_SHARE_TARGET) tips += TipCode.AddHeavyCompounds
            }

            GoalPrimaryGoalEnumState.BUILD_MUSCLE -> {
                if (progress.hypertrophyShare < GoalAdherence.FOCUS_SHARE_TARGET) tips += TipCode.IncreaseHypertrophyReps
            }

            GoalPrimaryGoalEnumState.LOSE_FAT -> {
                if (progress.enduranceShare < GoalAdherence.FOCUS_SHARE_TARGET) tips += TipCode.AddMetabolicWork
            }

            GoalPrimaryGoalEnumState.MAINTAIN -> {
                if (kotlin.math.abs(progress.strengthShare - progress.hypertrophyShare) > GoalAdherence.BALANCE_DELTA_LIMIT) {
                    tips += TipCode.BalanceStrengthAndHypertrophy
                }
                if (progress.enduranceShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) tips += TipCode.ReduceSuppressedAxis
            }

            GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
                if (kotlin.math.abs(progress.strengthShare - progress.enduranceShare) > GoalAdherence.BALANCE_DELTA_LIMIT) {
                    tips += TipCode.BalanceStrengthAndEndurance
                }
                if (progress.hypertrophyShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) tips += TipCode.ReduceSuppressedAxis
            }

            GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> {
                when {
                    score < GoalProgressState.DRIFTING_MIN -> tips += TipCode.RampUpGradually
                    score < GoalProgressState.ON_TRACK_MIN -> tips += TipCode.DialBackIntensity
                }
            }
        }

        // Timeline-related tips
        if (progress.daysRemaining < 0 && !progress.isFinished) {
            tips += TipCode.ReviewDeadline
        }

        // Always leave at least one encouraging tip
        if (tips.isEmpty()) {
            tips += if (score >= GoalProgressState.ON_TRACK_MIN) TipCode.StayTheCourse
            else TipCode.KeepConsistentFrequency
        } else if (tips.size < 3 && score < GoalProgressState.ON_TRACK_MIN) {
            tips += TipCode.KeepConsistentFrequency
        }

        return tips.distinct().toPersistentList()
    }

    override fun onBack() {
        navigateTo(TrainingGoalDetailsDirection.Back)
    }

    override fun onAddGoal() {
        navigateTo(TrainingGoalDetailsDirection.AddGoal)
    }
}
