package com.grippo.training.goal.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.stubGoalProgressList
import com.grippo.design.components.metrics.goal.GoalCard
import com.grippo.design.components.tip.TipCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_focus_endurance
import com.grippo.design.resources.provider.goal_details_focus_hypertrophy
import com.grippo.design.resources.provider.goal_details_focus_share
import com.grippo.design.resources.provider.goal_details_focus_strength
import com.grippo.design.resources.provider.goal_details_focus_subtitle
import com.grippo.design.resources.provider.goal_details_focus_title
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

    LazyColumn(
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
                FocusDistributionCard(
                    modifier = Modifier.fillMaxWidth(),
                    strengthShare = progress.strengthShare,
                    hypertrophyShare = progress.hypertrophyShare,
                    enduranceShare = progress.enduranceShare,
                )
            }
        }

        if (state.insights.isNotEmpty()) {
            item(key = "insights_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.goal_details_insights_title),
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary,
                )
            }

            itemsIndexed(
                items = state.insights,
                key = { index, _ -> "insight_$index" },
            ) { _, item ->
                InsightRow(
                    item = item
                )
            }
        }

        if (state.tips.isNotEmpty()) {
            item(key = "tips_header") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.goal_details_tips_title),
                    style = AppTokens.typography.h5(),
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

/**
 * A stacked horizontal bar + legend that shows how the user's training
 * breaks down across the three training qualities. It's information-dense
 * but readable: each colored segment is proportional to its share,
 * and the legend below shows the % number for every axis.
 */
@Composable
private fun FocusDistributionCard(
    modifier: Modifier = Modifier,
    strengthShare: Int,
    hypertrophyShare: Int,
    enduranceShare: Int,
) {
    val radius = AppTokens.dp.metrics.panel.small.radius
    val strengthColor = AppTokens.colors.charts.ring.warning.indicator
    val hypertrophyColor = AppTokens.colors.charts.ring.info.indicator
    val enduranceColor = AppTokens.colors.charts.ring.success.indicator

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(AppTokens.colors.background.card, RoundedCornerShape(radius))
            .padding(
                horizontal = AppTokens.dp.metrics.panel.small.horizontalPadding,
                vertical = AppTokens.dp.metrics.panel.small.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.panel.small.spacer),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_focus_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Text(
            text = AppTokens.strings.res(Res.string.goal_details_focus_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        StackedShareBar(
            modifier = Modifier.fillMaxWidth(),
            segments = persistentListOf(
                StackedSegment(strengthShare, strengthColor),
                StackedSegment(hypertrophyShare, hypertrophyColor),
                StackedSegment(enduranceShare, enduranceColor),
            ),
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_strength),
            percent = strengthShare,
            color = strengthColor,
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_hypertrophy),
            percent = hypertrophyShare,
            color = hypertrophyColor,
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_endurance),
            percent = enduranceShare,
            color = enduranceColor,
        )
    }
}

@Immutable
private data class StackedSegment(
    val weight: Int,
    val color: Color,
)

@Composable
private fun StackedShareBar(
    modifier: Modifier = Modifier,
    segments: List<StackedSegment>,
) {
    val barHeight = 10.dp
    val shape = RoundedCornerShape(barHeight / 2)
    val total = segments.sumOf { it.weight.coerceAtLeast(0) }
    val fallbackColor = AppTokens.colors.charts.ring.muted.track

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(shape)
            .background(fallbackColor, shape),
    ) {
        if (total <= 0) return@Row
        segments.forEach { segment ->
            val weight = segment.weight.coerceAtLeast(0).toFloat() / total.toFloat()
            if (weight <= 0f) return@forEach
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .background(segment.color),
            )
        }
    }
}

@Composable
private fun FocusLegendRow(
    label: String,
    percent: Int,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color, CircleShape),
        )

        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.goal_details_focus_share,
                percent.coerceIn(0, 100)
            ),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun InsightRow(item: InsightItem) {
    val (headline, detail) = item.reason.reasonText()
    val accent = item.severity.color()
    val radius = AppTokens.dp.metrics.panel.small.radius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(radius))
            .background(AppTokens.colors.background.card, RoundedCornerShape(radius))
            .padding(
                horizontal = AppTokens.dp.metrics.panel.small.horizontalPadding,
                vertical = AppTokens.dp.metrics.panel.small.verticalPadding,
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        // Vertical accent stripe communicating severity at a glance.
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent, RoundedCornerShape(2.dp)),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = headline,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = detail,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
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
