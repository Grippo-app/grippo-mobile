package com.grippo.training.goal.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.metrics.profile.goal.GoalCalculationBreakdownCard
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalFocusDistributionCard
import com.grippo.design.components.metrics.profile.goal.GoalInsightCard
import com.grippo.design.components.metrics.profile.goal.GoalInsightSeverity
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.components.tip.TipCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_insights_title
import com.grippo.design.resources.provider.goal_details_subtitle
import com.grippo.design.resources.provider.goal_details_tips_title
import com.grippo.design.resources.provider.goal_details_title
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

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.goal_details_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        text = AppTokens.strings.res(Res.string.goal_details_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val listState = rememberAnchoredLazyListState(
        behavior = AnchorScrollBehavior.Animated
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        val progress = state.progress

        if (progress != null) {
            item(key = "hero") {
                GoalCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = progress,
                    onUpdateClick = contract::onAddGoal
                )
            }

            item(key = "description") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = progress.goal.primaryGoal.description(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }

            item(key = "focus") {
                GoalFocusDistributionCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = progress,
                )
            }

            if (progress.hasBreakdown) {
                item(key = "breakdown") {
                    GoalCalculationBreakdownCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = progress,
                    )
                }
            }
        }

        if (state.insights.isNotEmpty()) {
            item(key = "insights_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.goal_details_insights_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }

            itemsIndexed(
                items = state.insights,
                key = { index, _ -> "insight_$index" },
            ) { _, item ->
                val (headline, detail) = item.reason.reasonText()
                GoalInsightCard(
                    modifier = Modifier.fillMaxWidth(),
                    severity = item.severity.toGoalInsightSeverity(),
                    headline = headline,
                    detail = detail,
                )
            }
        }

        if (state.tips.isNotEmpty()) {
            item(key = "tips_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.goal_details_tips_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }

            itemsIndexed(
                items = state.tips,
                key = { index, _ -> "tip_$index" },
            ) { _, code ->
                TipCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = code.tipText(),
                )
            }
        }

        item("bottom_space") {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private fun InsightItem.Severity.toGoalInsightSeverity(): GoalInsightSeverity = when (this) {
    InsightItem.Severity.Positive -> GoalInsightSeverity.Positive
    InsightItem.Severity.Warning -> GoalInsightSeverity.Warning
    InsightItem.Severity.Negative -> GoalInsightSeverity.Negative
    InsightItem.Severity.Neutral -> GoalInsightSeverity.Neutral
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
                    InsightItem(InsightItem.Severity.Negative, ReasonCode.StrengthShareLow),
                    InsightItem(InsightItem.Severity.Neutral, ReasonCode.AlmostDone),
                ),
                tips = persistentListOf(
                    TipCode.AddHeavyCompounds,
                    TipCode.KeepConsistentFrequency,
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
                tips = persistentListOf(TipCode.StayTheCourse),
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
                    InsightItem(InsightItem.Severity.Positive, ReasonCode.HypertrophyShareOk),
                ),
                tips = persistentListOf(TipCode.IncreaseHypertrophyReps),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}
