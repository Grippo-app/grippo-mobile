package com.grippo.design.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.HighlightMetric
import com.grippo.core.state.trainings.highlight.HighlightMuscleFocus
import com.grippo.core.state.trainings.highlight.HighlightMuscleFocusSegment
import com.grippo.core.state.trainings.highlight.HighlightPerformanceMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceStatus
import com.grippo.core.state.trainings.highlight.HighlightStreakMood
import com.grippo.core.state.trainings.highlight.HighlightStreakType
import com.grippo.core.state.trainings.highlight.stubHighlight
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.modifiers.fire
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.highlight_best_value
import com.grippo.design.resources.provider.highlight_focus_exercise
import com.grippo.design.resources.provider.highlight_muscle_focus
import com.grippo.design.resources.provider.highlight_status_declined
import com.grippo.design.resources.provider.highlight_status_improved
import com.grippo.design.resources.provider.highlight_status_record
import com.grippo.design.resources.provider.highlight_status_stable
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_streak_daily_target
import com.grippo.design.resources.provider.highlight_streak_mood_crushing
import com.grippo.design.resources.provider.highlight_streak_mood_on_track
import com.grippo.design.resources.provider.highlight_streak_mood_restart
import com.grippo.design.resources.provider.highlight_streak_rhythm
import com.grippo.design.resources.provider.highlight_streak_rhythm_target
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_secondary
import com.grippo.design.resources.provider.highlight_streak_weekly_target
import com.grippo.design.resources.provider.highlight_streaks
import com.grippo.design.resources.provider.highlight_type_comeback
import com.grippo.design.resources.provider.highlight_type_comeback_hint
import com.grippo.design.resources.provider.highlight_type_consistency
import com.grippo.design.resources.provider.highlight_type_consistency_hint
import com.grippo.design.resources.provider.highlight_type_momentum
import com.grippo.design.resources.provider.highlight_type_momentum_hint
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.icons.Intensity
import com.grippo.design.resources.provider.intensity_chip
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    value: Highlight,
    onViewWorkout: () -> Unit,
    onExampleClick: (id: String) -> Unit,
) {
    val storyType = run {
        val dominantMetric = value.performance.firstOrNull()
        val streakLength = value.streak.featured.length
        when {
            streakLength >= 3 -> HighlightStoryType.Consistency
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
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.home.highlights.icon),
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

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        val spacing = AppTokens.dp.contentPadding.content

        fun metricOf(type: HighlightMetric): HighlightPerformanceMetric? =
            value.performance.firstOrNull { it.metric == type }

        // Focus exercise - full width
        value.focusExercise?.let { example ->
            HighlightPanel(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(
                        text = AppTokens.strings.res(Res.string.highlight_focus_exercise),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val onExampleClickProvider = remember(example.value.id) {
                        { onExampleClick.invoke(example.value.id) }
                    }

                    ExerciseExampleCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = example,
                        style = ExerciseExampleCardStyle.Medium(
                            onClick = onExampleClickProvider,
                            allowUsageLabel = true
                        )
                    )
                }
            )

            Spacer(Modifier.height(spacing))
        }

        // Row: Muscle focus + Workout streak
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            value.muscleFocus?.let { muscle ->
                HighlightPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content = { HighlightMuscleFocusPanel(muscle) }
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            HighlightPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(AppTokens.dp.home.highlights.panel.radius))
                    .fire(
                        intensity = when (value.streak.featured.mood) {
                            HighlightStreakMood.CrushingIt -> 0.5f
                            HighlightStreakMood.OnTrack -> 0.3f
                            HighlightStreakMood.Restart -> 0.2f
                        },
                        frequency = when (value.streak.featured.mood) {
                            HighlightStreakMood.CrushingIt -> 0.8f
                            HighlightStreakMood.OnTrack -> 0.7f
                            HighlightStreakMood.Restart -> 0.7f
                        },
                        speed =  when (value.streak.featured.mood) {
                            HighlightStreakMood.CrushingIt -> 0.7f
                            HighlightStreakMood.OnTrack -> 0.7f
                            HighlightStreakMood.Restart -> 0.7f
                        },
                    ),
                content = { HighlightStreakPanel(value) }
            )
        }

        Spacer(Modifier.height(spacing))

        // Row: Duration + Volume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(HighlightMetric.Duration)?.let { metric ->
                HighlightPanel(
                    modifier = Modifier.weight(1f),
                    content = { HighlightPerformancePrimaryMetric(metric) }
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(HighlightMetric.Volume)?.let { metric ->
                HighlightPanel(
                    modifier = Modifier.weight(1f),
                    content = { HighlightPerformancePrimaryMetric(metric) }
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(spacing))

        // Row: Reps + Intensity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            metricOf(HighlightMetric.Repetitions)?.let { metric ->
                HighlightPanel(
                    modifier = Modifier.weight(1f),
                    content = { HighlightPerformancePrimaryMetric(metric) }
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            metricOf(HighlightMetric.Intensity)?.let { metric ->
                HighlightPanel(
                    modifier = Modifier.weight(1f),
                    content = { HighlightPerformancePrimaryMetric(metric) }
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HighlightPanel(
    modifier: Modifier = Modifier,
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .clip(RoundedCornerShape(AppTokens.dp.home.highlights.panel.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.home.highlights.panel.radius)
            )
    ) {
        decoration?.invoke(this)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTokens.dp.contentPadding.content),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            content = content
        )
    }
}

@Composable
private fun HighlightMuscleFocusPanel(muscle: HighlightMuscleFocus) {
    Text(
        text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
        muscle.segments.forEachIndexed { index, segment ->
            HighlightMuscleFocusSegmentRow(
                segment = segment,
                isDominant = index == 0
            )
        }
    }
}

@Composable
private fun HighlightMuscleFocusSegmentRow(
    segment: HighlightMuscleFocusSegment,
    isDominant: Boolean,
) {
    val progress = segment.load.value
        ?.coerceIn(0, 100)
        ?.toFloat()
        ?.div(100f) ?: 0f
    val spacing = if (isDominant) {
        AppTokens.dp.contentPadding.text
    } else {
        AppTokens.dp.contentPadding.text
    }
    val labelStyle = if (isDominant) {
        AppTokens.typography.h5()
    } else {
        AppTokens.typography.b13Med()
    }
    val valueStyle = if (isDominant) {
        AppTokens.typography.h5()
    } else {
        AppTokens.typography.b13Semi()
    }
    val labelColor = if (isDominant) {
        AppTokens.colors.text.primary
    } else {
        AppTokens.colors.text.secondary
    }
    val valueColor = if (isDominant) {
        AppTokens.colors.text.primary
    } else {
        AppTokens.colors.text.secondary
    }
    val indicatorColors = if (isDominant) {
        AppTokens.colors.lineIndicator.success
    } else {
        AppTokens.colors.lineIndicator.primary
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = segment.muscleGroup.title().text(),
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = segment.load.short(),
                style = valueStyle,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progress,
            colors = indicatorColors
        )
    }
}

@Composable
private fun HighlightStreakPanel(value: Highlight) {
    val title = AppTokens.strings.res(Res.string.highlight_streaks)
    val (headlinePrimary, headlineSecondary) = when (value.streak.featured.type) {
        HighlightStreakType.Daily -> AppTokens.strings.res(
            Res.string.highlight_streak,
            value.streak.featured.length
        ) to null

        HighlightStreakType.Weekly -> AppTokens.strings.res(
            Res.string.highlight_streak_weekly_headline_primary,
            value.streak.featured.length
        ) to AppTokens.strings.res(
            Res.string.highlight_streak_weekly_headline_secondary,
            value.streak.featured.targetSessionsPerPeriod
        )

        HighlightStreakType.Rhythm -> {
            val rhythm = value.streak.featured.rhythm ?: return
            AppTokens.strings.res(
                Res.string.highlight_streak_rhythm,
                rhythm.workDays,
                rhythm.restDays
            ) to null
        }
    }
    val cadenceLabel = when (value.streak.featured.type) {
        HighlightStreakType.Daily -> AppTokens.strings.res(Res.string.highlight_streak_daily_target)
        HighlightStreakType.Weekly -> AppTokens.strings.res(
            Res.string.highlight_streak_weekly_target,
            value.streak.featured.targetSessionsPerPeriod
        )

        HighlightStreakType.Rhythm -> {
            val rhythm = value.streak.featured.rhythm ?: return
            AppTokens.strings.res(
                Res.string.highlight_streak_rhythm_target,
                rhythm.workDays,
                rhythm.restDays
            )
        }
    }
    val encouragement = when (value.streak.featured.mood) {
        HighlightStreakMood.CrushingIt -> AppTokens.strings.res(Res.string.highlight_streak_mood_crushing)
        HighlightStreakMood.OnTrack -> AppTokens.strings.res(Res.string.highlight_streak_mood_on_track)
        HighlightStreakMood.Restart -> AppTokens.strings.res(Res.string.highlight_streak_mood_restart)
    }
    val progressValue = (value.streak.featured.progressPercent.coerceIn(0, 100)) / 100f

    val progressColors = when (value.streak.featured.mood) {
        HighlightStreakMood.CrushingIt -> AppTokens.colors.lineIndicator.success
        HighlightStreakMood.OnTrack -> AppTokens.colors.lineIndicator.info
        HighlightStreakMood.Restart -> AppTokens.colors.lineIndicator.warning
    }

    Text(
        text = title,
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Column {
        Text(
            text = headlinePrimary,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        headlineSecondary?.let { secondary ->
            Text(
                text = secondary,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Text(
        text = cadenceLabel,
        style = AppTokens.typography.b13Med(),
        color = AppTokens.colors.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    LineIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = progressValue,
        colors = progressColors
    )

    Text(
        text = AppTokens.strings.res(
            Res.string.highlight_active_days,
            value.streak.totalActiveDays
        ),
        style = AppTokens.typography.b13Med(),
        color = AppTokens.colors.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.home.highlights.performance.icon),
                imageVector = metric.metric.icon(),
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null
            )

            Text(
                text = label,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

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

@Immutable
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
            onViewWorkout = {},
            onExampleClick = {}
        )
    }
}
