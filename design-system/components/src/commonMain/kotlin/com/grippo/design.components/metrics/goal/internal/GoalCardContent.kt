package com.grippo.design.components.metrics.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.core.state.metrics.stubGoalProgressList
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_adherence_score
import com.grippo.design.resources.provider.goal_card_title

@Composable
internal fun GoalCardContent(
    value: GoalProgressState
) {
    val score = value.score.coerceIn(0, 100)
    val ringColors = ringColors(score = score, isFinished = value.isFinished)
    val indicatorColors = indicatorColors(score = score, isFinished = value.isFinished)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalTitleBlock(
            modifier = Modifier.weight(1f)
                .fillMaxHeight(),
            title = value.goal.primaryGoal.label(),
            score = score,
            isFinished = value.isFinished,
            statusText = value.statusLabel()
        )

        AdherenceRing(
            modifier = Modifier.size(AppTokens.dp.metrics.goal.chart),
            score = score,
            ringColors = ringColors,
        )
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        LineIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTokens.dp.contentPadding.text),
            progress = value.progressFraction,
            colors = indicatorColors,
            labelSpacing = AppTokens.dp.contentPadding.text,
            startLabel = {
                Text(
                    text = value.goal.createdAt.display,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            endLabel = {
                Text(
                    text = value.goal.target.display,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            marker = {
                Text(
                    text = "\uD83D\uDD25\uFE0F",
                    style = AppTokens.typography.h4(),
                )
            },
        )

        Text(
            text = "${value.progressLine()} · ${value.remainingLine()}",
            style = AppTokens.typography.b13Med(),
            color = if (value.daysRemaining < 0) {
                AppTokens.colors.semantic.warning
            } else {
                AppTokens.colors.text.secondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GoalTitleBlock(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    score: Int,
    isFinished: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_card_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val pillShape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)
        val statusColor = statusColor(score = score, isFinished = isFinished)

        Text(
            modifier = Modifier
                .clip(pillShape)
                .background(statusColor.copy(alpha = 0.20f), shape = pillShape)
                .padding(
                    horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                    vertical = AppTokens.dp.metrics.status.verticalPadding,
                ),
            text = statusText,
            style = AppTokens.typography.b11Semi(),
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AdherenceRing(
    modifier: Modifier = Modifier,
    score: Int,
    ringColors: AppColor.Charts.RingColor.RingPalette,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = score.toFloat(),
            max = 100f,
            colors = ringColors,
        )

        Text(
            text = AppTokens.strings.res(Res.string.goal_card_adherence_score, score),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@Composable
private fun statusColor(
    score: Int,
    isFinished: Boolean,
): Color = when {
    isFinished && score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.semantic.success
    score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.semantic.success
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.semantic.warning
    else -> AppTokens.colors.semantic.error
}

@Composable
private fun ringColors(
    score: Int,
    isFinished: Boolean,
): AppColor.Charts.RingColor.RingPalette = when {
    isFinished && score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.ring.success
    score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.ring.success
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.charts.ring.warning
    else -> AppTokens.colors.charts.ring.error
}

@Composable
private fun indicatorColors(
    score: Int,
    isFinished: Boolean,
): AppColor.Charts.IndicatorColors.IndicatorColors = when {
    isFinished && score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.indicator.success
    score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.indicator.success
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.charts.indicator.warning
    else -> AppTokens.colors.charts.indicator.error
}

@AppPreview
@Composable
private fun GoalCardContentOnTrackPreview() {
    PreviewContainer {
        val value =
            stubGoalProgressList().first { !it.isFinished && it.score >= GoalProgressState.ON_TRACK_MIN }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = { GoalCardContent(value = value) }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentDriftingPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first {
            !it.isFinished &&
                    it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
        }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = { GoalCardContent(value = value) }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOffTrackPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score < GoalProgressState.DRIFTING_MIN }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = { GoalCardContent(value = value) }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOverduePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.daysRemaining < 0 }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = { GoalCardContent(value = value) }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentCompletedPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.isFinished }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = { GoalCardContent(value = value) }
        )
    }
}
