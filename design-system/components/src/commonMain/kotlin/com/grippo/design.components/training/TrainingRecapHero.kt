package com.grippo.design.components.training

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
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
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.sets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val RECAP_FACT_COUNT = 3
private const val RECAP_BACKDROP_REVEAL_MS = 900
private const val RECAP_BACKDROP_SWITCH_MS = 250
private const val RECAP_BACKDROP_REVEAL_LABEL = "RecapBackdropReveal"
private const val RECAP_BACKDROP_SWITCH_LABEL = "RecapBackdropSwitch"

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
    windowInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
) {
    val durationValue = training.duration.value
    val volumeValue = training.total.volume.value
    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { exercise -> exercise.iterations.size }
    }
    val resolvedVerdict = (style as? TrainingRecapHeroStyle.Resolved)?.verdict

    RecapHeroSurface(
        modifier = modifier,
        windowInsets = windowInsets,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                when (style) {
                    TrainingRecapHeroStyle.Pending -> {
                        val textMeasurer = rememberTextMeasurer()

                        RecapTitleSkeleton(textMeasurer = textMeasurer)
                        RecapDescriptionSkeleton(textMeasurer = textMeasurer)
                    }

                    is TrainingRecapHeroStyle.Resolved -> {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { heading() },
                            text = style.verdict.title(baseline = style.baseline),
                            style = AppTokens.typography.h2(),
                            color = AppTokens.colors.text.primary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = style.description,
                            style = AppTokens.typography.b14Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
            }

            RecapBackdrop(verdict = resolvedVerdict)
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
public fun TrainingRecapHeroSkeleton(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
) {
    val textMeasurer = rememberTextMeasurer()

    RecapHeroSurface(
        modifier = modifier,
        windowInsets = windowInsets,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                RecapTitleSkeleton(textMeasurer = textMeasurer)
                RecapDescriptionSkeleton(textMeasurer = textMeasurer)
            }

            RecapBackdrop(verdict = null)
        }

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
private fun RecapHeroSurface(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(
        bottomStart = AppTokens.dp.brandHeader.radius,
        bottomEnd = AppTokens.dp.brandHeader.radius,
    )

    Column(
        modifier = modifier
            .illustrationBackground(shape)
            .windowInsetsPadding(windowInsets)
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding + AppTokens.dp.brandHeader.horizontalPadding,
                vertical = AppTokens.dp.brandHeader.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        content = content,
    )
}

@Composable
private fun RecapBackdrop(verdict: PerformanceVerdictState?) {
    Box(modifier = Modifier.size(AppTokens.dp.illustration.compact)) {
        if (verdict != null) {
            var revealed by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) { revealed = true }
            val reveal = animateFloatAsState(
                targetValue = if (revealed) 1f else 0f,
                animationSpec = tween(durationMillis = RECAP_BACKDROP_REVEAL_MS, easing = FastOutSlowInEasing),
                label = RECAP_BACKDROP_REVEAL_LABEL,
            )
            val rise = with(LocalDensity.current) { AppTokens.dp.training.recap.backdrop.rise.toPx() }

            Crossfade(
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * rise
                },
                targetState = verdict,
                animationSpec = tween(durationMillis = RECAP_BACKDROP_SWITCH_MS),
                label = RECAP_BACKDROP_SWITCH_LABEL,
            ) { shown ->
                Illustration(
                    painter = rememberVectorPainter(shown.rememberBackdrop()),
                    style = IllustrationStyle.Compact,
                )
            }
        }
    }
}

@Composable
private fun RecapTitleSkeleton(textMeasurer: TextMeasurer) {
    SkeletonText(
        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
        style = AppTokens.typography.h2(),
        textMeasurer = textMeasurer,
    )
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
private fun TrainingRecapHeroPendingPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = TrainingRecapHeroStyle.Pending,
        )
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
private fun TrainingRecapHeroPreviousBaselinePreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubTrainingSummary(),
            style = previewResolvedStyle(
                metrics = stubPerformanceMetricsImproved(),
                baseline = PerformanceBaselineState.Previous,
            ),
        )
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
