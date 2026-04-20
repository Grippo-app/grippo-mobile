package com.grippo.design.components.metrics.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_adherence_score
import com.grippo.design.resources.provider.goal_card_title

@Composable
internal fun GoalCardContent(value: GoalProgressState) {
    val score = value.score.coerceIn(0, 100)
    val statusColor = statusColor(score = score, isFinished = value.isFinished)
    val ringColors = ringColors(score = score, isFinished = value.isFinished)
    val indicatorColors = indicatorColors(score = score, isFinished = value.isFinished)

    GoalHeader(
        statusText = value.statusLabel(),
        statusColor = statusColor,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalTitleBlock(
            modifier = Modifier.weight(1f),
            title = value.goal.primaryGoal.label(),
            metaText = "${value.progressLine()} · ${value.remainingLine()}",
            metaColor = if (value.daysRemaining < 0) {
                AppTokens.colors.semantic.warning
            } else {
                AppTokens.colors.text.secondary
            },
        )

        AdherenceRing(
            modifier = Modifier.size(AppTokens.dp.metrics.goal.chart),
            score = score,
            ringColors = ringColors,
        )
    }

    LineIndicator(
        modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun GoalHeader(
    statusText: String,
    statusColor: Color,
) {
    val pillShape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.goal_card_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

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
    }
}

@Composable
private fun GoalTitleBlock(
    modifier: Modifier = Modifier,
    title: String,
    metaText: String,
    metaColor: Color,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = title,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = metaText,
            style = AppTokens.typography.b13Med(),
            color = metaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.charts.ring.info
    else -> AppTokens.colors.charts.ring.warning
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
