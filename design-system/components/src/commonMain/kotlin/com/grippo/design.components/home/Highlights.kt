package com.grippo.design.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.HighlightExerciseFocus
import com.grippo.core.state.trainings.highlight.HighlightMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceStatus
import com.grippo.core.state.trainings.highlight.stubHighlight
import com.grippo.design.components.modifiers.spot
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
import com.grippo.design.resources.provider.highlight_story_comeback
import com.grippo.design.resources.provider.highlight_story_consistency
import com.grippo.design.resources.provider.highlight_story_momentum
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_trend
import com.grippo.design.resources.provider.highlight_type_comeback
import com.grippo.design.resources.provider.highlight_type_consistency
import com.grippo.design.resources.provider.highlight_type_momentum
import com.grippo.design.resources.provider.highlight_unique_exercises
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.design.resources.provider.weight
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    value: Highlight,
    onViewWorkout: () -> Unit
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(AppTokens.dp.home.highlights.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.home.highlights.radius)
            )
    ) {
        Image(
            modifier = Modifier
                .spot(color = AppTokens.colors.brand.color3)
                .align(Alignment.CenterEnd)
                .size(AppTokens.dp.home.highlights.image),
            painter = AppTokens.drawables.res(Res.drawable.weight),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = AppTokens.dp.home.highlights.verticalPadding,
                    horizontal = AppTokens.dp.home.highlights.horizontalPadding
                )
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.highlights),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            HighlightStorySection(
                value = value,
                type = value.storyType()
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            val summaryItems = buildList {
                add(
                    HighlightSummaryItem(
                        label = AppTokens.strings.res(Res.string.trainings),
                        value = value.trainingsCount.toString()
                    )
                )

                add(
                    HighlightSummaryItem(
                        label = AppTokens.strings.res(Res.string.highlight_unique_exercises),
                        value = value.uniqueExercises.toString()
                    )
                )

                value.totalVolume.takeUnless { it is VolumeFormatState.Empty }?.short()
                    ?.let { volume ->
                        add(
                            HighlightSummaryItem(
                                label = AppTokens.strings.res(Res.string.volume),
                                value = volume
                            )
                        )
                    }

                add(
                    HighlightSummaryItem(
                        label = AppTokens.strings.res(Res.string.duration),
                        value = DateTimeUtils.format(value.totalDuration)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                summaryItems.forEach { item ->
                    HighlightSummaryStat(
                        modifier = Modifier.weight(1f),
                        label = item.label,
                        value = item.value
                    )
                }
            }

            value.focusExercise?.let { focus ->
                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

                HighlightDetailRow(
                    title = AppTokens.strings.res(Res.string.highlight_focus_exercise),
                    headline = focus.name,
                    supporting = focusSupportingText(focus),
                    trailing = focus.totalVolume.short()
                )
            }

            value.muscleFocus?.let { muscle ->
                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

                HighlightDetailRow(
                    title = AppTokens.strings.res(Res.string.highlight_muscle_focus),
                    headline = muscle.muscleGroup.title().text(),
                    supporting = muscle.load.short(),
                )
            }

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                HighlightDetailRow(
                    modifier = Modifier.weight(1f),
                    title = AppTokens.strings.res(Res.string.highlight_consistency),
                    headline = AppTokens.strings.res(
                        Res.string.highlight_streak,
                        value.consistency.bestStreakDays
                    ),
                    supporting = AppTokens.strings.res(
                        Res.string.highlight_active_days,
                        value.consistency.activeDays
                    )
                )

                if (value.performance.isNotEmpty()) {
                    HighlightPerformanceSection(
                        modifier = Modifier.weight(1f),
                        metrics = value.performance
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightSummaryStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        Text(
            text = value,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.primary
        )
    }
}

private data class HighlightSummaryItem(
    val label: String,
    val value: String,
)

@Composable
private fun HighlightPerformanceSection(
    modifier: Modifier = Modifier,
    metrics: List<HighlightPerformanceMetric>,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_trend),
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary
        )

        metrics.forEach { metric ->
            HighlightPerformanceMetricRow(metric)
        }
    }
}

@Composable
private fun HighlightPerformanceMetricRow(metric: HighlightPerformanceMetric) {
    val label = when (metric.metric) {
        HighlightMetric.Volume -> AppTokens.strings.res(Res.string.volume)
        HighlightMetric.Duration -> AppTokens.strings.res(Res.string.duration)
    }
    val statusLabel = performanceStatusLabel(metric.status)
    val statusColor = performanceStatusColor(metric.status)
    val delta = formatTrendDelta(metric.deltaPercentage)
    val vsAverage = AppTokens.strings.res(Res.string.highlight_vs_average)
    val (current, average, bestLabel) = when (metric) {
        is HighlightPerformanceMetric.Volume -> {
            val best = AppTokens.strings.res(
                Res.string.highlight_best_value,
                metric.best.short()
            )
            Triple(metric.current.short(), metric.average.short(), best)
        }

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
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text / 2)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary
            )

            Text(
                text = statusLabel,
                style = AppTokens.typography.b11Semi(),
                color = statusColor
            )
        }

        Text(
            text = delta,
            style = AppTokens.typography.h5(),
            color = if (metric.status == HighlightPerformanceStatus.Declined)
                AppTokens.colors.semantic.warning
            else
                AppTokens.colors.text.primary
        )

        Text(
            text = "$current · $vsAverage $average",
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
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
private fun focusSupportingText(focus: HighlightExerciseFocus): String {
    val sessions = AppTokens.strings.res(Res.string.highlight_sessions, focus.sessions)
    val forceWeight = forceWeightLabel(focus.forceType, focus.weightType)
    return "$sessions · $forceWeight"
}

@Composable
private fun forceWeightLabel(
    forceType: ForceTypeEnumState,
    weightType: WeightTypeEnumState,
): String {
    val force = forceType.title().text()
    val weight = weightType.title().text()
    return AppTokens.strings.res(Res.string.highlight_force_weight, force, weight)
}

@Composable
private fun HighlightStorySection(
    value: Highlight,
    type: HighlightStoryType,
) {
    val title = when (type) {
        HighlightStoryType.Consistency -> AppTokens.strings.res(Res.string.highlight_type_consistency)
        HighlightStoryType.Momentum -> AppTokens.strings.res(Res.string.highlight_type_momentum)
        HighlightStoryType.Comeback -> AppTokens.strings.res(Res.string.highlight_type_comeback)
    }

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
                    Res.string.highlight_story_comeback,
                    value.trainingsCount,
                    DateTimeUtils.format(value.totalDuration)
                )
            } else {
                val metric = when (dominant.metric) {
                    HighlightMetric.Volume -> AppTokens.strings.res(Res.string.volume)
                    HighlightMetric.Duration -> AppTokens.strings.res(Res.string.duration)
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
            Res.string.highlight_story_comeback,
            value.trainingsCount,
            DateTimeUtils.format(value.totalDuration)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = title,
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.brand.color6
        )

        Text(
            text = description,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

private enum class HighlightStoryType {
    Consistency, Momentum, Comeback
}

@Composable
private fun performanceStatusLabel(status: HighlightPerformanceStatus): String {
    return when (status) {
        HighlightPerformanceStatus.Record ->
            AppTokens.strings.res(Res.string.highlight_status_record)

        HighlightPerformanceStatus.Improved ->
            AppTokens.strings.res(Res.string.highlight_status_improved)

        HighlightPerformanceStatus.Stable ->
            AppTokens.strings.res(Res.string.highlight_status_stable)

        HighlightPerformanceStatus.Declined ->
            AppTokens.strings.res(Res.string.highlight_status_declined)
    }
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

private fun Highlight.storyType(): HighlightStoryType {
    val dominantMetric = performance.firstOrNull()
    return when {
        consistency.bestStreakDays >= 4 -> HighlightStoryType.Consistency
        dominantMetric?.status == HighlightPerformanceStatus.Record ||
                dominantMetric?.status == HighlightPerformanceStatus.Improved -> HighlightStoryType.Momentum

        else -> HighlightStoryType.Comeback
    }
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
