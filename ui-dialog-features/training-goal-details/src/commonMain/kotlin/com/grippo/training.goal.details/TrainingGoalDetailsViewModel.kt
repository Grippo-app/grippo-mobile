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
                            severity = InsightItem.Severity.Neutral,
                            reason = ReasonCode.NoData,
                        )
                    ),
                )
            }
            return
        }

        val progress = adherence.toState()
        val insights = buildInsights(progress)

        update {
            it.copy(
                progress = progress,
                insights = insights,
            )
        }
    }

    /**
     * Produces the full insight list for [progress]. Each insight is one
     * observation about the user's adherence; if there is a concrete next
     * step paired to that observation, it is attached as `action`.
     *
     * No fallback "always show something" item is emitted — if the data
     * does not warrant a finding, the corresponding slot is silent.
     */
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
                lowAction = TipCode.AddHeavyCompounds,
            )

            GoalPrimaryGoalEnumState.BUILD_MUSCLE -> items += focusAxis(
                share = progress.hypertrophyShare,
                lowReason = ReasonCode.HypertrophyShareLow,
                okReason = ReasonCode.HypertrophyShareOk,
                lowAction = TipCode.IncreaseHypertrophyReps,
            )

            GoalPrimaryGoalEnumState.LOSE_FAT -> items += focusAxis(
                share = progress.enduranceShare,
                lowReason = ReasonCode.EnduranceShareLow,
                okReason = ReasonCode.EnduranceShareOk,
                lowAction = TipCode.AddMetabolicWork,
            )

            GoalPrimaryGoalEnumState.MAINTAIN -> {
                items += balanceAxis(
                    a = progress.strengthShare,
                    b = progress.hypertrophyShare,
                    driftAction = TipCode.BalanceStrengthAndHypertrophy,
                )
                if (progress.enduranceShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) {
                    items += InsightItem(
                        severity = InsightItem.Severity.Warning,
                        reason = ReasonCode.SuppressedAxisTooHigh,
                        action = TipCode.ReduceSuppressedAxis,
                    )
                }
            }

            GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
                items += balanceAxis(
                    a = progress.strengthShare,
                    b = progress.enduranceShare,
                    driftAction = TipCode.BalanceStrengthAndEndurance,
                )
                if (progress.hypertrophyShare > GoalAdherence.SUPPRESSED_AXIS_LIMIT) {
                    items += InsightItem(
                        severity = InsightItem.Severity.Warning,
                        reason = ReasonCode.SuppressedAxisTooHigh,
                        action = TipCode.ReduceSuppressedAxis,
                    )
                }
            }

            GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> items += when {
                score < GoalProgressState.DRIFTING_MIN && progress.daysElapsed >= GoalAdherence.EARLY_DAYS ->
                    InsightItem(
                        severity = InsightItem.Severity.Warning,
                        reason = ReasonCode.ReturnToTrainingUnderWork,
                        action = TipCode.RampUpGradually,
                    )

                score < GoalProgressState.ON_TRACK_MIN ->
                    InsightItem(
                        severity = InsightItem.Severity.Warning,
                        reason = ReasonCode.ReturnToTrainingOverWork,
                        action = TipCode.DialBackIntensity,
                    )

                else ->
                    InsightItem(
                        severity = InsightItem.Severity.Positive,
                        reason = ReasonCode.ReturnToTrainingInRange,
                    )
            }
        }

        // 3) Timeline notes
        when {
            isFinished -> {
                // already covered above
            }

            progress.daysRemaining < 0 ->
                items += InsightItem(
                    severity = InsightItem.Severity.Warning,
                    reason = ReasonCode.Overdue,
                    action = TipCode.ReviewDeadline,
                )

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
        lowAction: TipCode,
    ): InsightItem = when {
        share >= GoalAdherence.FOCUS_SHARE_STRONG ->
            InsightItem(InsightItem.Severity.Positive, okReason)

        share >= GoalAdherence.FOCUS_SHARE_TARGET ->
            InsightItem(InsightItem.Severity.Neutral, okReason)

        else ->
            InsightItem(InsightItem.Severity.Negative, lowReason, lowAction)
    }

    private fun balanceAxis(
        a: Int,
        b: Int,
        driftAction: TipCode,
    ): InsightItem {
        val delta = kotlin.math.abs(a - b)
        return if (delta <= GoalAdherence.BALANCE_DELTA_LIMIT) {
            InsightItem(InsightItem.Severity.Positive, ReasonCode.BalanceOk)
        } else {
            InsightItem(InsightItem.Severity.Warning, ReasonCode.BalanceDrift, driftAction)
        }
    }

    override fun onBack() {
        navigateTo(TrainingGoalDetailsDirection.Back)
    }

    override fun onAddGoal() {
        navigateTo(TrainingGoalDetailsDirection.AddGoal)
    }
}
