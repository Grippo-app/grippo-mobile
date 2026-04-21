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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.core.state.metrics.stubGoalProgressList
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

/**
 * Shows how the user's training is split across the three training qualities
 * (strength / hypertrophy / endurance) as a stacked proportional bar plus a
 * percent legend.
 *
 * All dimensions come from `AppTokens.dp.metrics.goal.focusDistribution` so
 * the card stays consistent with the rest of the goal widgets.
 */
@Composable
public fun GoalFocusDistributionCard(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    val strength = value.strengthShare
    val hypertrophy = value.hypertrophyShare
    val endurance = value.enduranceShare

    val strengthColor = AppTokens.colors.charts.ring.warning.indicator
    val hypertrophyColor = AppTokens.colors.charts.ring.info.indicator
    val enduranceColor = AppTokens.colors.charts.ring.success.indicator

    val radius = AppTokens.dp.metrics.goal.focusDistribution.radius

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(AppTokens.colors.background.card, RoundedCornerShape(radius))
            .padding(
                horizontal = AppTokens.dp.metrics.goal.focusDistribution.horizontalPadding,
                vertical = AppTokens.dp.metrics.goal.focusDistribution.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.goal.focusDistribution.spacer),
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

        Spacer(modifier = Modifier.size(AppTokens.dp.metrics.goal.focusDistribution.titleToBar))

        StackedShareBar(
            modifier = Modifier.fillMaxWidth(),
            segments = listOf(
                StackedShareSegment(strength, strengthColor),
                StackedShareSegment(hypertrophy, hypertrophyColor),
                StackedShareSegment(endurance, enduranceColor),
            ),
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_strength),
            percent = strength,
            color = strengthColor,
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_hypertrophy),
            percent = hypertrophy,
            color = hypertrophyColor,
        )

        FocusLegendRow(
            label = AppTokens.strings.res(Res.string.goal_details_focus_endurance),
            percent = endurance,
            color = enduranceColor,
        )
    }
}

@Immutable
private data class StackedShareSegment(
    val weight: Int,
    val color: Color,
)

@Composable
private fun StackedShareBar(
    modifier: Modifier = Modifier,
    segments: List<StackedShareSegment>,
) {
    val barHeight = AppTokens.dp.metrics.goal.focusDistribution.barHeight
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
                    .fillMaxHeight()
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
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.goal.focusDistribution.legendSpacing),
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.metrics.goal.focusDistribution.legendDot)
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

@AppPreview
@Composable
private fun GoalFocusDistributionCardPreview() {
    PreviewContainer {
        GoalFocusDistributionCard(value = stubGoalProgressList().random())
    }
}
