package com.grippo.design.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.HighlightMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceStatus
import com.grippo.core.state.trainings.highlight.stubHighlight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.highlight_best_value
import com.grippo.design.resources.provider.highlight_consistency
import com.grippo.design.resources.provider.highlight_focus_exercise
import com.grippo.design.resources.provider.highlight_force_weight
import com.grippo.design.resources.provider.highlight_muscle_focus
import com.grippo.design.resources.provider.highlight_sessions
import com.grippo.design.resources.provider.highlight_status_declined
import com.grippo.design.resources.provider.highlight_status_improved
import com.grippo.design.resources.provider.highlight_status_record
import com.grippo.design.resources.provider.highlight_status_stable
import com.grippo.design.resources.provider.highlight_story_consistency
import com.grippo.design.resources.provider.highlight_story_momentum
import com.grippo.design.resources.provider.highlight_story_work
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_type_comeback
import com.grippo.design.resources.provider.highlight_type_comeback_hint
import com.grippo.design.resources.provider.highlight_type_consistency
import com.grippo.design.resources.provider.highlight_type_consistency_hint
import com.grippo.design.resources.provider.highlight_type_momentum
import com.grippo.design.resources.provider.highlight_type_momentum_hint
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.intensity_chip
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    value: Highlight,
    onViewWorkout: () -> Unit
) {
    val storyType = run {
        val dominantMetric = value.performance.firstOrNull()
        when {
            value.consistency.bestStreakDays >= 4 -> HighlightStoryType.Consistency
            dominantMetric?.status == HighlightPerformanceStatus.Record ||
                    dominantMetric?.status == HighlightPerformanceStatus.Improved -> HighlightStoryType.Momentum

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
        val storyChipShape = RoundedCornerShape(AppTokens.dp.home.highlights.status.radius)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.highlights),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                modifier = Modifier
                    .clip(storyChipShape)
                    .background(
                        AppTokens.colors.text.primary.copy(alpha = 0.08f),
                        shape = storyChipShape
                    )
                    .padding(
                        horizontal = AppTokens.dp.home.highlights.status.horizontalPadding,
                        vertical = AppTokens.dp.home.highlights.status.verticalPadding
                    ),
                text = "$storyTitle · $storyHint",
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        HighlightStorySection(
            value = value,
            type = storyType
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        val spacing = AppTokens.dp.contentPadding.content
        fun metricOf(type: HighlightMetric): HighlightPerformanceMetric? =
            value.performance.firstOrNull { it.metric == type }

        // Focus exercise - full width
        value.focusExercise?.let { focus ->
            HighlightPanel(modifier = Modifier.fillMaxWidth()) {
                val sessions =
                    AppTokens.strings.res(Res.string.highlight_sessions, focus.sessions)
                val force = focus.forceType.title().text()
                val weight = focus.weightType.title().text()
                val forceWeight =
                    AppTokens.strings.res(Res.string.highlight_force_weight, force, weight)

                Text(
                    text = AppTokens.strings.res(Res.string.highlight_focus_exercise),
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = focus.name,
                        style = AppTokens.typography.h5(),
                        color = AppTokens.colors.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = focus.totalVolume.short(),
                        style = AppTokens.typography.b14Semi(),
                        color = AppTokens.colors.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = "$sessions · $forceWeight",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(spacing))
        }

        // Row: Muscle focus + Consistency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            value.muscleFocus?.let { muscle ->
                HighlightPanel(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
                        style = AppTokens.typography.b11Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = muscle.muscleGroup.title().text(),
                        style = AppTokens.typography.h5(),
                        color = AppTokens.colors.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = muscle.load.short(),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            HighlightPanel(modifier = Modifier.weight(1f)) {
                HighlightConsistencyPanel(value)
            }
        }

        Spacer(Modifier.height(spacing))

        // Row: Duration + Volume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(HighlightMetric.Duration)?.let { metric ->
                HighlightPanel(modifier = Modifier.weight(1f)) {
                    HighlightPerformancePrimaryMetric(metric)
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(HighlightMetric.Volume)?.let { metric ->
                HighlightPanel(modifier = Modifier.weight(1f)) {
                    HighlightPerformancePrimaryMetric(metric)
                }
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(spacing))

        // Row: Reps + Intensity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(HighlightMetric.Repetitions)?.let { metric ->
                HighlightPanel(modifier = Modifier.weight(1f)) {
                    HighlightPerformancePrimaryMetric(metric)
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(HighlightMetric.Intensity)?.let { metric ->
                HighlightPanel(modifier = Modifier.weight(1f)) {
                    HighlightPerformancePrimaryMetric(metric)
                }
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HighlightPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppTokens.dp.home.highlights.status.radius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.card, shape = shape)
            .padding(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        content = content
    )
}

@Composable
private fun HighlightConsistencyPanel(value: Highlight) {
    Text(
        text = AppTokens.strings.res(Res.string.highlight_consistency),
        style = AppTokens.typography.b11Med(),
        color = AppTokens.colors.text.secondary
    )

    Text(
        text = AppTokens.strings.res(Res.string.highlight_streak, value.consistency.bestStreakDays),
        style = AppTokens.typography.h5(),
        color = AppTokens.colors.text.primary
    )

    Text(
        text = AppTokens.strings.res(
            Res.string.highlight_active_days,
            value.consistency.activeDays
        ),
        style = AppTokens.typography.b13Med(),
        color = AppTokens.colors.text.secondary
    )
}

@Composable
private fun HighlightStatusChip(status: HighlightPerformanceStatus) {
    val label = when (status) {
        HighlightPerformanceStatus.Record ->
            AppTokens.strings.res(Res.string.highlight_status_record)

        HighlightPerformanceStatus.Improved ->
            AppTokens.strings.res(Res.string.highlight_status_improved)

        HighlightPerformanceStatus.Stable ->
            AppTokens.strings.res(Res.string.highlight_status_stable)

        HighlightPerformanceStatus.Declined ->
            AppTokens.strings.res(Res.string.highlight_status_declined)
    }

    val color = performanceStatusColor(status)

    val shape = RoundedCornerShape(AppTokens.dp.home.highlights.status.radius)

    Text(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.2f), shape = shape)
            .padding(
                horizontal = AppTokens.dp.home.highlights.status.horizontalPadding,
                vertical = AppTokens.dp.home.highlights.status.verticalPadding
            ),
        text = label,
        style = AppTokens.typography.b11Semi(),
        color = color
    )
}

@Composable
private fun performanceStatusColor(status: HighlightPerformanceStatus): Color {
    return when (status) {
        HighlightPerformanceStatus.Record,
        HighlightPerformanceStatus.Improved -> AppTokens.colors.semantic.success

        HighlightPerformanceStatus.Stable -> AppTokens.colors.text.secondary

        HighlightPerformanceStatus.Declined -> AppTokens.colors.semantic.warning
    }
}

@Composable
private fun HighlightPerformancePrimaryMetric(metric: HighlightPerformanceMetric) {
    val label = when (metric.metric) {
        HighlightMetric.Duration -> AppTokens.strings.res(Res.string.duration)
        HighlightMetric.Volume -> AppTokens.strings.res(Res.string.volume)
        HighlightMetric.Repetitions -> AppTokens.strings.res(Res.string.repetitions)
        HighlightMetric.Intensity -> AppTokens.strings.res(Res.string.intensity_chip)
    }
    val delta = formatTrendDelta(metric.deltaPercentage)
    val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)
    val (current, average, bestLabel) = when (metric) {
        is HighlightPerformanceMetric.Duration -> {
            val best = AppTokens.strings.res(
                Res.string.highlight_best_value,
                DateTimeUtils.format(metric.best)
            )
            Triple(
                DateTimeUtils.format(metric.current),
                DateTimeUtils.format(metric.average),
                best
            )
        }

        is HighlightPerformanceMetric.Volume -> {
            val best = AppTokens.strings.res(
                Res.string.highlight_best_value,
                metric.best.short()
            )
            Triple(metric.current.short(), metric.average.short(), best)
        }

        is HighlightPerformanceMetric.Repetitions -> {
            val best = AppTokens.strings.res(
                Res.string.highlight_best_value,
                metric.best.short()
            )
            Triple(metric.current.short(), metric.average.short(), best)
        }

        is HighlightPerformanceMetric.Intensity -> {
            val best = AppTokens.strings.res(
                Res.string.highlight_best_value,
                metric.best.short()
            )
            Triple(metric.current.short(), metric.average.short(), best)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text / 2)
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = delta,
                style = AppTokens.typography.h5(),
                color = performanceStatusColor(metric.status)
            )

            HighlightStatusChip(status = metric.status)
        }

        Text(
            text = "$current · $vsAverage $average",
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = bestLabel,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary
        )
    }
}

@Composable
private fun HighlightDetailRow(
    modifier: Modifier = Modifier,
    title: String,
    headline: String,
    supporting: String?,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            Text(
                text = headline,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary
            )

            supporting?.let {
                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    text = it,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary
                )
            }
        }

        trailing?.let {
            Text(
                text = it,
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary
            )
        }
    }
}

@Composable
private fun HighlightStorySection(
    value: Highlight,
    type: HighlightStoryType,
) {
    val description = when (type) {
        HighlightStoryType.Consistency -> AppTokens.strings.res(
            Res.string.highlight_story_consistency,
            value.consistency.activeDays,
            value.consistency.bestStreakDays
        )

        HighlightStoryType.Momentum -> {
            val dominant = value.performance.firstOrNull()
            if (dominant == null) {
                AppTokens.strings.res(
                    Res.string.highlight_story_work,
                    DateTimeUtils.format(value.totalDuration)
                )
            } else {
                val metric = when (dominant.metric) {
                    HighlightMetric.Volume -> AppTokens.strings.res(Res.string.volume)
                    HighlightMetric.Duration -> AppTokens.strings.res(Res.string.duration)
                    HighlightMetric.Repetitions -> AppTokens.strings.res(Res.string.reps)
                    HighlightMetric.Intensity -> AppTokens.strings.res(Res.string.intensity_chip)
                }
                val delta = formatTrendDelta(dominant.deltaPercentage)
                AppTokens.strings.res(
                    Res.string.highlight_story_momentum,
                    metric,
                    delta
                )
            }
        }

        HighlightStoryType.Comeback -> AppTokens.strings.res(
            Res.string.highlight_story_work,
            DateTimeUtils.format(value.totalDuration)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = description,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

private enum class HighlightStoryType {
    Consistency,
    Momentum,
    Comeback
}

private fun formatTrendDelta(delta: Int): String {
    return when {
        delta > 0 -> "+${delta}%"
        delta < 0 -> "${delta}%"
        else -> "0%"
    }
}

@AppPreview
@Composable
private fun HighlightsCardPreview() {
    PreviewContainer {
        HighlightsCard(
            value = stubHighlight(),
            onViewWorkout = {}
        )
    }
}
