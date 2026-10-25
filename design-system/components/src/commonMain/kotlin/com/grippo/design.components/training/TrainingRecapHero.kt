package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceVerdictState
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsDeclined
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsImproved
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutBaseline
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTrainingSummary
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.button.internal.rememberTextButtonSize
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Share
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.share
import com.grippo.design.resources.provider.statistics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val RECAP_FACT_COUNT = 3

@Immutable
public sealed interface TrainingRecapHeroStyle {
    @Immutable
    public data object Pending : TrainingRecapHeroStyle

    @Immutable
    public data class Resolved(
        public val verdict: PerformanceVerdictState,
        public val baseline: PerformanceBaselineState,
        public val description: String,
    ) : TrainingRecapHeroStyle
}

@Composable
public fun TrainingRecapHero(
    modifier: Modifier = Modifier,
    training: TrainingState,
    style: TrainingRecapHeroStyle,
) {
    val durationValue = training.duration.value
    val volumeValue = training.total.volume.value
    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { exercise -> exercise.iterations.size }
    }

    RecapHeroSurface(modifier = modifier) {
        when (style) {
            TrainingRecapHeroStyle.Pending -> {
                val textMeasurer = rememberTextMeasurer()

                RecapVerdictRowSkeleton(textMeasurer = textMeasurer)
                RecapDescriptionSkeleton(textMeasurer = textMeasurer)
            }

            is TrainingRecapHeroStyle.Resolved -> {
                RecapVerdictRow(
                    verdict = style.verdict,
                    title = style.verdict.title(baseline = style.baseline),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = style.description,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            if (durationValue != null) {
                RecapFact(
                    modifier = Modifier.weight(1f),
                    value = DurationFormatState.shortMinutes(durationValue),
                    label = PerformanceMetricTypeState.Duration.label(),
                )
            }
            if (volumeValue != null) {
                RecapFact(
                    modifier = Modifier.weight(1f),
                    value = VolumeFormatState.shortTotal(volumeValue),
                    label = PerformanceMetricTypeState.Volume.label(),
                )
            }
            RecapFact(
                modifier = Modifier.weight(1f),
                value = setsCount.toString(),
                label = AppTokens.strings.res(Res.string.sets),
                dimmed = setsCount == 0,
            )
        }
    }
}

@Composable
public fun TrainingRecapActions(
    modifier: Modifier = Modifier,
    shareEnabled: Boolean,
    onStatisticsClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val shareLabel = AppTokens.strings.res(Res.string.share)
    val shareState = if (shareEnabled) ButtonState.Enabled else ButtonState.Disabled

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = shareLabel },
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Share)),
            size = ButtonSize.Small,
            style = ButtonStyle.Secondary,
            state = shareState,
            onClick = onShareClick,
        )
        Button(
            content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.statistics)),
            size = ButtonSize.Small,
            style = ButtonStyle.Secondary,
            onClick = onStatisticsClick,
        )
    }
}

@Composable
public fun TrainingRecapHeroSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    RecapHeroSurface(modifier = modifier) {
        RecapVerdictRowSkeleton(textMeasurer = textMeasurer)
        RecapDescriptionSkeleton(textMeasurer = textMeasurer)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            repeat(RECAP_FACT_COUNT) {
                RecapFactSkeleton(modifier = Modifier.weight(1f), textMeasurer = textMeasurer)
            }
        }
    }
}

@Composable
public fun TrainingRecapActionsSkeleton(modifier: Modifier = Modifier) {
    val statisticsSize = rememberTextButtonSize(
        text = AppTokens.strings.res(Res.string.statistics),
        size = ButtonSize.Small,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.minimumInteractiveComponentSize(),
            contentAlignment = Alignment.Center,
        ) {
            SkeletonBone(
                modifier = Modifier.size(AppTokens.dp.button.small.height),
                shape = CircleShape,
                animated = false,
            )
        }
        SkeletonBone(
            modifier = Modifier.size(statisticsSize),
            shape = RoundedCornerShape(AppTokens.dp.button.small.radius),
            animated = false,
        )
    }
}

@Composable
private fun RecapHeroSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .illustrationBackground(RoundedCornerShape(AppTokens.dp.brandHeader.radius))
            .padding(
                horizontal = AppTokens.dp.brandHeader.horizontalPadding,
                vertical = AppTokens.dp.brandHeader.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        content = content,
    )
}

@Composable
private fun RecapVerdictRow(
    verdict: PerformanceVerdictState,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.training.recap.verdict.space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.training.recap.verdict.tile.size)
                .clip(RoundedCornerShape(AppTokens.dp.training.recap.verdict.tile.radius))
                .background(AppTokens.colors.background.card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.training.recap.verdict.tile.icon),
                imageVector = verdict.icon(),
                tint = verdict.color(),
                contentDescription = null,
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@Composable
private fun RecapVerdictRowSkeleton(textMeasurer: TextMeasurer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.training.recap.verdict.space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBone(
            modifier = Modifier.size(AppTokens.dp.training.recap.verdict.tile.size),
            shape = RoundedCornerShape(AppTokens.dp.training.recap.verdict.tile.radius),
            animated = false,
        )
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
            style = AppTokens.typography.h2(),
            textMeasurer = textMeasurer,
        )
    }
}

@Composable
private fun RecapDescriptionSkeleton(textMeasurer: TextMeasurer) {
    SkeletonText(
        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
        style = AppTokens.typography.b14Med(),
        textMeasurer = textMeasurer,
    )
}

@Composable
private fun RecapFact(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    dimmed: Boolean = false,
) {
    val description = "$label, $value"

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h3(),
            color = if (dimmed) AppTokens.colors.text.tertiary else AppTokens.colors.text.primary,
        )
        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun RecapFactSkeleton(
    modifier: Modifier = Modifier,
    textMeasurer: TextMeasurer,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
            style = AppTokens.typography.h3(),
            textMeasurer = textMeasurer,
        )
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
            style = AppTokens.typography.b11Med(),
            textMeasurer = textMeasurer,
        )
    }
}

@Composable
private fun previewResolvedStyle(
    metrics: ImmutableList<PerformanceMetricState>,
    baseline: PerformanceBaselineState = PerformanceBaselineState.Average,
): TrainingRecapHeroStyle.Resolved {
    val verdict = PerformanceVerdictState.of(metrics)
    return TrainingRecapHeroStyle.Resolved(
        verdict = verdict,
        baseline = baseline,
        description = verdict.summary(metrics = metrics, baseline = baseline),
    )
}

@AppPreview
@Composable
private fun TrainingRecapHeroRecordPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsByType()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroImprovedPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsImproved()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroStablePreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = TrainingRecapHeroStyle.Resolved(
                verdict = PerformanceVerdictState.Stable,
                baseline = PerformanceBaselineState.Average,
                description = PerformanceBaselineState.Average.comparedSummary(improved = 2, compared = 4),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroDeclinedPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsDeclined()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroFirstWorkoutPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsWithoutBaseline()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroPendingWithActionsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            TrainingRecapHero(
                modifier = Modifier.fillMaxWidth(),
                training = stubTrainingSummary(),
                style = TrainingRecapHeroStyle.Pending,
            )
            TrainingRecapActions(
                modifier = Modifier.fillMaxWidth(),
                shareEnabled = false,
                onStatisticsClick = {},
                onShareClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroSkeletonPreview() {
    PreviewContainer {
        TrainingRecapHeroSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroWithActionsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            TrainingRecapHero(
                modifier = Modifier.fillMaxWidth(),
                training = stubTrainingSummary(),
                style = previewResolvedStyle(
                    metrics = stubPerformanceMetricsImproved(),
                    baseline = PerformanceBaselineState.Previous,
                ),
            )
            TrainingRecapActions(
                modifier = Modifier.fillMaxWidth(),
                shareEnabled = true,
                onStatisticsClick = {},
                onShareClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingRecapActionsSkeletonPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            TrainingRecapActionsSkeleton(modifier = Modifier.fillMaxWidth())
            TrainingRecapActions(
                modifier = Modifier.fillMaxWidth(),
                shareEnabled = true,
                onStatisticsClick = {},
                onShareClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroLongValuesPreview() {
    val training = stubTrainingSummary()
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = training.copy(
                duration = DurationFormatState.of(23.hours + 59.minutes),
                total = training.total.copy(
                    volume = VolumeFormatState.ofTotal(12_345_678f),
                    repetitions = RepetitionsFormatState.ofTotal(123_456),
                ),
            ),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsByType()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroMissingFactsPreview() {
    val training = stubTrainingSummary()
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = training.copy(
                duration = DurationFormatState.Empty(),
                exercises = persistentListOf(),
                total = training.total.copy(volume = VolumeFormatState.Empty()),
            ),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsWithoutBaseline()),
        )
    }
}
