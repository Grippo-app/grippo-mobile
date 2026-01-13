package com.grippo.design.components.metrics

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.stubExerciseSpotlight
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.modifiers.scalableClick
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    totalDuration: Duration,
    spotlight: ExerciseSpotlightState?,
    muscleLoad: MuscleLoadSummaryState?,
    streak: TrainingStreakState,
    performance: List<PerformanceMetricState>,
    onExampleClick: (id: String) -> Unit,
    onStreakClick: () -> Unit,
    onMuscleLoadingClick: () -> Unit,
    onPerformanceMetricClick: (type: PerformanceMetricTypeState) -> Unit
) {
    val storyType = run {
        val dominantMetric = performance.firstOrNull()
        val streakLength = streak.featured.length
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
            performance.firstOrNull { it.type == type }

        spotlight?.let { spotlightValue ->
            val onExampleClickProvider = remember(spotlightValue.exercise.value.id) {
                { onExampleClick.invoke(spotlightValue.exercise.value.id) }
            }

            ExerciseSpotlightCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scalableClick(onClick = onExampleClickProvider),
                value = spotlightValue,
            )

            Spacer(Modifier.height(spacing))
        }

        val hasMuscleLoad = muscleLoad?.perGroup?.entries?.isNotEmpty() == true
        if (hasMuscleLoad) {
            Row(
                modifier = Modifier
                    .height(intrinsicSize = IntrinsicSize.Max)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                MuscleLoadingCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scalableClick(onClick = onMuscleLoadingClick),
                    summary = muscleLoad
                )

                TrainingStreakCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scalableClick(onClick = onStreakClick),
                    value = streak
                )
            }
        } else {
            TrainingStreakCard(
                modifier = Modifier.fillMaxWidth(),
                value = streak
            )
        }

        val performanceRows = listOf(
            listOfNotNull(
                metricOf(PerformanceMetricTypeState.Density),
                metricOf(PerformanceMetricTypeState.Volume)
            ),
            listOfNotNull(
                metricOf(PerformanceMetricTypeState.Repetitions),
                metricOf(PerformanceMetricTypeState.Intensity)
            ),
        ).filter { row -> row.isNotEmpty() }

        if (performanceRows.isNotEmpty()) {
            Spacer(Modifier.height(spacing))

            performanceRows.forEachIndexed { index, metrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    metrics.forEach { metric ->
                        val onPerformanceMetricClickProvider = remember {
                            { onPerformanceMetricClick.invoke(metric.type) }
                        }
                        PerformanceTrendCard(
                            modifier = Modifier
                                .weight(1f)
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = metric
                        )
                    }
                }

                if (index < performanceRows.lastIndex) {
                    Spacer(Modifier.height(spacing))
                }
            }
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
private fun HighlightsCardFullPreview() {
    PreviewContainer {
        HighlightsCard(
            totalDuration = 28.hours,
            spotlight = stubExerciseSpotlight(),
            muscleLoad = stubMuscleLoadSummary(),
            streak = stubTrainingStreaks().first(),
            performance = stubPerformanceMetrics(),
            onExampleClick = {},
            onStreakClick = {},
            onMuscleLoadingClick = {},
            onPerformanceMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun HighlightsCardSpotlightOnlyPreview() {
    PreviewContainer {
        HighlightsCard(
            totalDuration = 12.hours,
            spotlight = stubExerciseSpotlight(),
            muscleLoad = stubMuscleLoadSummary(),
            streak = stubTrainingStreaks().first(),
            performance = stubPerformanceMetrics().take(5),
            onExampleClick = {},
            onStreakClick = {},
            onMuscleLoadingClick = {},
            onPerformanceMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun HighlightsCardMinimalPreview() {
    PreviewContainer {
        HighlightsCard(
            totalDuration = 6.hours,
            spotlight = null,
            muscleLoad = null,
            streak = stubTrainingStreaks().first(),
            performance = emptyList(),
            onExampleClick = {},
            onMuscleLoadingClick = {},
            onStreakClick = {},
            onPerformanceMetricClick = {},
        )
    }
}
