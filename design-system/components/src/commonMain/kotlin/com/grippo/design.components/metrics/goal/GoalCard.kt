package com.grippo.design.components.metrics.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.grippo.core.state.metrics.stubGoalProgress
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_adherence_label
import com.grippo.design.resources.provider.goal_card_adherence_score
import com.grippo.design.resources.provider.goal_card_description_1
import com.grippo.design.resources.provider.goal_card_description_2
import com.grippo.design.resources.provider.goal_card_no_data
import com.grippo.design.resources.provider.goal_card_title

@Composable
public fun GoalCard(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    val score = value.adherence.score.coerceIn(0, 100)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_card_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value.primaryGoal.label(),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = value.statusLabel(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

                Text(
                    text = value.progressLine(),
                    style = AppTokens.typography.b13Semi(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    text = value.remainingLine(),
                    style = AppTokens.typography.b13Med(),
                    color = remainingColor(value),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AdherenceRing(
                modifier = Modifier.size(AppTokens.dp.metrics.goal.chart),
                score = score,
                caption = AppTokens.strings.res(Res.string.goal_card_adherence_label),
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        ProgressTimeline(
            modifier = Modifier.fillMaxWidth(),
            fraction = value.progressFraction,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value.startedLabel(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = value.targetLabel(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            if (score == 0) {
                Text(
                    text = AppTokens.strings.res(Res.string.goal_card_no_data),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.tertiary,
                )
            }

            Text(
                text = AppTokens.strings.res(Res.string.goal_card_description_1),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
            )

            Text(
                text = AppTokens.strings.res(Res.string.goal_card_description_2),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun remainingColor(value: GoalProgressState): Color {
    return when {
        value.daysRemaining < 0 -> AppTokens.colors.semantic.warning
        else -> AppTokens.colors.text.secondary
    }
}

@Composable
private fun AdherenceRing(
    score: Int,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val colors = when {
        score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.lineIndicator.success
        score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.lineIndicator.info
        else -> AppTokens.colors.lineIndicator.warning
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = score.toFloat(),
            max = 100f,
            colors = colors,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.goal_card_adherence_score, score),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = caption,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressTimeline(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val trackColor = AppTokens.colors.lineIndicator.info.track
    val fillColor = AppTokens.colors.lineIndicator.info.indicator
    val height = AppTokens.dp.metrics.goal.progressBar.height
    val radius = AppTokens.dp.metrics.goal.progressBar.radius

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(RoundedCornerShape(radius))
                .background(fillColor),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardPreview() {
    PreviewContainer {
        GoalCard(value = stubGoalProgress())
    }
}
