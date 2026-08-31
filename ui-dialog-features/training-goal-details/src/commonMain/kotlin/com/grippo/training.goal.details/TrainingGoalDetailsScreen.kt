package com.grippo.training.goal.details

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.InsightItem
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.ReasonCode
import com.grippo.training.goal.details.TrainingGoalDetailsDialogState.TipCode
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingGoalDetailsScreen(
    state: TrainingGoalDetailsDialogState,
    loaders: ImmutableSet<TrainingGoalDetailsLoader>,
    contract: TrainingGoalDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenPreview() {
    val progress = stubGoalProgressList().first { it.score in 40..69 }
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                progress = progress,
                insights = persistentListOf(
                    InsightItem(InsightItem.Severity.Warning, ReasonCode.ScoreDrifting),
                    InsightItem(
                        severity = InsightItem.Severity.Negative,
                        reason = ReasonCode.StrengthShareLow,
                        action = TipCode.AddHeavyCompounds,
                    ),
                    InsightItem(InsightItem.Severity.Neutral, ReasonCode.AlmostDone),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenOnTrackPreview() {
    val progress = stubGoalProgressList().first { it.score >= 70 }
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                progress = progress,
                insights = persistentListOf(
                    InsightItem(InsightItem.Severity.Positive, ReasonCode.ScoreOnTrack),
                    InsightItem(InsightItem.Severity.Positive, ReasonCode.StrengthShareOk),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenHypertrophyPreview() {
    val progress = stubGoalProgress(primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE)
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                progress = progress,
                insights = persistentListOf(
                    InsightItem(
                        severity = InsightItem.Severity.Negative,
                        reason = ReasonCode.HypertrophyShareLow,
                        action = TipCode.IncreaseHypertrophyReps,
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}
