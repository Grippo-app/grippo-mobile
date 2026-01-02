package com.grippo.design.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.HighlightState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.core.state.metrics.stubHighlight
import com.grippo.design.components.metrics.ExerciseSpotlightSection
import com.grippo.design.components.metrics.MuscleLoadSection
import com.grippo.design.components.metrics.PerformanceTrendSection
import com.grippo.design.components.metrics.TrainingStreakSection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_type_comeback
import com.grippo.design.resources.provider.highlight_type_comeback_hint
import com.grippo.design.resources.provider.highlight_type_consistency
import com.grippo.design.resources.provider.highlight_type_consistency_hint
import com.grippo.design.resources.provider.highlight_type_momentum
import com.grippo.design.resources.provider.highlight_type_momentum_hint
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.icons.Intensity

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    value: HighlightState,
    onViewWorkout: () -> Unit,
    onExampleClick: (id: String) -> Unit,
) {
    val storyType = run {
        val dominantMetric = value.performance.firstOrNull()
        val streakLength = value.streak.featured.length
        when {
            streakLength >= 3 -> HighlightStoryType.Consistency
            dominantMetric?.status == PerformanceTrendStatusState.Record ||
                    dominantMetric?.status == PerformanceTrendStatusState.Improved -> HighlightStoryType.Momentum

            else -> HighlightStoryType.Comeback
        }
    }

    Column(modifier = modifier) {
        val storyTitle = when (storyType) {
            HighlightStoryType.Consistency -> AppTokens.strings.res(Res.string.highlight_type_consistency)
            HighlightStoryType.Momentum -> AppTokens.strings.res(Res.string.highlight_type_momentum)
            HighlightStoryType.Comeback -> AppTokens.strings.res(Res.string.highlight_type_comeback)
        }
        val storyHint = when (storyType) {
            HighlightStoryType.Consistency -> AppTokens.strings.res(Res.string.highlight_type_consistency_hint)
            HighlightStoryType.Momentum -> AppTokens.strings.res(Res.string.highlight_type_momentum_hint)
            HighlightStoryType.Comeback -> AppTokens.strings.res(Res.string.highlight_type_comeback_hint)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.highlights.icon),
                imageVector = AppTokens.icons.Intensity,
                tint = AppTokens.colors.semantic.warning,
                contentDescription = null
            )

            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.highlights),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.semantic.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                modifier = Modifier
                    .background(
                        AppTokens.colors.text.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)
                    )
                    .padding(
                        horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                        vertical = AppTokens.dp.metrics.status.verticalPadding
                    ),
                text = "$storyTitle · $storyHint",
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        val spacing = AppTokens.dp.contentPadding.content

        fun metricOf(type: PerformanceMetricTypeState): PerformanceMetricState? =
            value.performance.firstOrNull { it.type == type }

        value.spotlight?.let { spotlight ->
            ExerciseSpotlightSection(
                modifier = Modifier.fillMaxWidth(),
                value = spotlight,
                onExampleClick = onExampleClick,
            )
            Spacer(Modifier.height(spacing))
        }

        Row(
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Max)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            val muscleLoad = value.muscleLoad

            if (muscleLoad != null && muscleLoad.perGroup.entries.isNotEmpty()) {
                MuscleLoadSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    summary = muscleLoad
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            TrainingStreakSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                value = value.streak
            )
        }

        Spacer(Modifier.height(spacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(PerformanceMetricTypeState.Duration)?.let { metric ->
                PerformanceTrendSection(
                    modifier = Modifier.weight(1f),
                    metric = metric
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(PerformanceMetricTypeState.Volume)?.let { metric ->
                PerformanceTrendSection(
                    modifier = Modifier.weight(1f),
                    metric = metric
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(spacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(PerformanceMetricTypeState.Repetitions)?.let { metric ->
                PerformanceTrendSection(
                    modifier = Modifier.weight(1f),
                    metric = metric
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(PerformanceMetricTypeState.Intensity)?.let { metric ->
                PerformanceTrendSection(
                    modifier = Modifier.weight(1f),
                    metric = metric
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Immutable
private enum class HighlightStoryType {
    Consistency,
    Momentum,
    Comeback
}

@AppPreview
@Composable
private fun HighlightsCardPreview() {
    PreviewContainer {
        HighlightsCard(
            value = stubHighlight(),
            onViewWorkout = {},
            onExampleClick = {}
        )
    }
}
