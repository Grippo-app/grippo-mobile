package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.highlight_status_record
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.tonnage
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    training: TrainingState,
    muscleLoad: MuscleLoadSummaryState,
    volumeTrend: PerformanceMetricState.Volume?,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        SummaryHero(
            modifier = Modifier.fillMaxWidth(),
            training = training,
            volumeTrend = volumeTrend,
        )
        SummaryBreakdown(
            modifier = Modifier.fillMaxWidth(),
            training = training,
        )
        MuscleLoading(
            modifier = Modifier.fillMaxWidth(),
            summary = muscleLoad,
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@Composable
private fun SummaryHero(
    modifier: Modifier = Modifier,
    training: TrainingState,
    volumeTrend: PerformanceMetricState.Volume?,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            modifier = Modifier.weight(weight = 1f, fill = false),
            text = AppTokens.strings.res(Res.string.tonnage),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.weight(weight = 1f, fill = false),
            text = training.total.volume.short(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.semantic.notice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        volumeTrend?.takeIf { it.hasMeaningfulDelta }?.let { trend ->
            TrendBadge(trend = trend)
        }
    }
}

/**
 * A trend is worth surfacing only when the comparison carries information:
 * Record always qualifies (best session ever) and Improved/Declined qualify
 * when the rounded delta is non-zero. Stable and rounded-to-zero deltas are
 * visual noise next to a hero number and get dropped.
 */
private val PerformanceMetricState.Volume.hasMeaningfulDelta: Boolean
    get() = when (status) {
        PerformanceTrendStatusState.Record -> true
        PerformanceTrendStatusState.Improved,
        PerformanceTrendStatusState.Declined -> currentVsAveragePercentage != 0

        PerformanceTrendStatusState.Stable -> false
    }

@Composable
private fun TrendBadge(
    modifier: Modifier = Modifier,
    trend: PerformanceMetricState.Volume,
) {
    val accent = trendAccent(trend.status)
    val icon = trendIcon(trend.status)
    val label = trendLabel(trend)
    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    Row(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.14f), shape = shape)
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.performance.trend.icon),
                imageVector = icon,
                tint = accent,
                contentDescription = null,
            )
        }

        Text(
            text = label,
            style = AppTokens.typography.b12Semi(),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun trendAccent(status: PerformanceTrendStatusState): Color {
    return when (status) {
        PerformanceTrendStatusState.Record,
        PerformanceTrendStatusState.Improved -> AppTokens.colors.semantic.success

        PerformanceTrendStatusState.Stable -> AppTokens.colors.text.secondary
        PerformanceTrendStatusState.Declined -> AppTokens.colors.semantic.warning
    }
}

@Composable
private fun trendIcon(status: PerformanceTrendStatusState): ImageVector? {
    return when (status) {
        PerformanceTrendStatusState.Record -> AppTokens.icons.Trophy
        PerformanceTrendStatusState.Improved -> AppTokens.icons.ArrowTop
        PerformanceTrendStatusState.Declined -> AppTokens.icons.ArrowDown
        PerformanceTrendStatusState.Stable -> null
    }
}

@Composable
private fun trendLabel(trend: PerformanceMetricState.Volume): String {
    if (trend.status == PerformanceTrendStatusState.Record) {
        return AppTokens.strings.res(Res.string.highlight_status_record)
    }

    val delta = trend.currentVsAveragePercentage
    val signed = when {
        delta > 0 -> "+${delta}%"
        delta < 0 -> "${delta}%"
        else -> "0%"
    }
    val vsAvg = AppTokens.strings.res(Res.string.highlight_vs_average)
    return "$signed $vsAvg"
}

@Composable
private fun SummaryBreakdown(
    modifier: Modifier = Modifier,
    training: TrainingState,
) {
    val durationLabel = AppTokens.strings.res(Res.string.duration)
    val setsLabel = AppTokens.strings.res(Res.string.sets)
    val repsLabel = AppTokens.strings.res(Res.string.reps)

    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { exercise -> exercise.iterations.size }
    }

    val repsCount = training.total.repetitions.value ?: 0

    MetricBreakdownRow(
        modifier = modifier,
        items = persistentListOf(
            MetricBreakdownItem(
                label = setsLabel,
                value = setsCount.toString(),
                dimmed = setsCount == 0,
            ),
            MetricBreakdownItem(
                label = repsLabel,
                value = repsCount.toString(),
                dimmed = repsCount == 0,
            ),
            MetricBreakdownItem(
                label = durationLabel,
                value = training.duration.display.ifBlank { "0" },
                dimmed = training.duration.display.isBlank(),
            ),
        ),
    )
}

@AppPreview
@Composable
private fun TrainingSummaryCardImprovedPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            volumeTrend = previewVolumeTrend(
                status = PerformanceTrendStatusState.Improved,
                deltaPercent = 12,
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardRecordPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            volumeTrend = previewVolumeTrend(
                status = PerformanceTrendStatusState.Record,
                deltaPercent = 34,
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardDeclinedPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            volumeTrend = previewVolumeTrend(
                status = PerformanceTrendStatusState.Declined,
                deltaPercent = -9,
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardStablePreview() {
    // Stable collapses to no badge (see hasMeaningfulDelta). Preview verifies
    // the hero row still lays out cleanly when the badge is absent.
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            volumeTrend = previewVolumeTrend(
                status = PerformanceTrendStatusState.Stable,
                deltaPercent = 0,
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardNoTrendPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            volumeTrend = null,
            onClick = {},
        )
    }
}

private fun previewVolumeTrend(
    status: PerformanceTrendStatusState,
    deltaPercent: Int,
): PerformanceMetricState.Volume {
    val current = VolumeFormatState.of(3_250f)
    return PerformanceMetricState.Volume(
        deltaPercentage = deltaPercent,
        currentVsAveragePercentage = deltaPercent,
        current = current,
        average = current,
        best = current,
        status = status,
    )
}
