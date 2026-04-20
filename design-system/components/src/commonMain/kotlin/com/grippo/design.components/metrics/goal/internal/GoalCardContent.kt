package com.grippo.design.components.metrics.goal.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_adherence_label
import com.grippo.design.resources.provider.goal_card_adherence_score
import com.grippo.design.resources.provider.goal_card_title

@Composable
internal fun GoalCardContent(value: GoalProgressState) {
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
                text = value.goal.primaryGoal.label(),
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

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            Text(
                text = value.progressLine(),
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = value.remainingLine(),
                style = AppTokens.typography.b13Med(),
                color = when {
                    value.daysRemaining < 0 -> AppTokens.colors.semantic.warning
                    else -> AppTokens.colors.text.secondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AdherenceRing(
            modifier = Modifier.size(AppTokens.dp.metrics.goal.chart),
            score = value.score.coerceIn(0, 100)
        )
    }

    Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

    LineIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = value.progressFraction,
        colors = AppTokens.colors.charts.indicator.success,
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
private fun AdherenceRing(
    modifier: Modifier = Modifier,
    score: Int
) {
    val colors = when {
        score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.ring.success
        score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.charts.ring.info
        else -> AppTokens.colors.charts.ring.warning
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = AppTokens.strings.res(Res.string.goal_card_adherence_score, score),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

            Text(
                text = AppTokens.strings.res(Res.string.goal_card_adherence_label),
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}
