package com.grippo.design.components.metrics.performance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetrics
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutBaseline
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.performance.internal.PerformanceTrendChip
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.performance_comparison_empty
import com.grippo.design.resources.provider.performance_comparison_first_title
import com.grippo.design.resources.provider.performance_comparison_title
import com.grippo.design.resources.provider.performance_growth_before
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val comparisonDisplayOrder: ImmutableList<PerformanceMetricTypeState> = persistentListOf(
    PerformanceMetricTypeState.Volume,
    PerformanceMetricTypeState.Repetitions,
    PerformanceMetricTypeState.Intensity,
    PerformanceMetricTypeState.Density,
)

private const val GROWTH_REVEAL_DURATION_MS = 900
private const val GROWTH_REVEAL_STAGGER_MS = 120
private const val GROWTH_SWITCH_DURATION_MS = 250
private const val GROWTH_REVEAL_LABEL = "PerformanceGrowthReveal"
private const val GROWTH_VALUE_LABEL = "PerformanceGrowthValue"
private const val GROWTH_AFTER_LABEL = "PerformanceGrowthAfter"
private const val GROWTH_FILL_LABEL = "PerformanceGrowthFill"
private const val GROWTH_TRACK_LABEL = "PerformanceGrowthTrack"
private const val GROWTH_BEFORE_LABEL = "PerformanceGrowthBefore"
private const val GROWTH_CROSSFADE_LABEL = "PerformanceGrowthCrossfade"

@Composable
public fun PerformanceComparisonCard(
    modifier: Modifier = Modifier,
    metrics: ImmutableList<PerformanceMetricState>,
    baseline: PerformanceBaselineState,
    onBaselineSelect: (PerformanceBaselineState) -> Unit,
    onMetricClick: (PerformanceMetricTypeState) -> Unit,
) {
    val orderedMetrics = remember(metrics) {
        comparisonDisplayOrder.mapNotNull { type -> metrics.firstOrNull { it.type == type } }
    }
    val hasValues = orderedMetrics.any { !it.isEmpty() }
    val hasBaseline = orderedMetrics.any { it.hasAverage() }
    val baselineItems = remember {
        PerformanceBaselineState.entries.map { entry -> entry to entry.labelText() }.toPersistentList()
    }
    val nameLineHeight = rememberTextHeight(style = AppTokens.typography.b13Semi())

    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal = updateTransition(targetState = revealed, label = GROWTH_REVEAL_LABEL)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Text(
            text = AppTokens.strings.res(
                if (hasValues && !hasBaseline) {
                    Res.string.performance_comparison_first_title
                } else {
                    Res.string.performance_comparison_title
                },
            ),
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        if (!hasValues) {
            Text(
                text = AppTokens.strings.res(Res.string.performance_comparison_empty),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            return@MetricSectionPanel
        }

        if (hasBaseline) {
            Segment(
                modifier = Modifier.fillMaxWidth(),
                items = baselineItems,
                style = SegmentStyle.Outline,
                selected = baseline,
                onSelect = onBaselineSelect,
                segmentWidth = SegmentWidth.EqualFill,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            orderedMetrics.forEachIndexed { index, metric ->
                key(metric.type) {
                    if (index > 0) {
                        ContentDivider()
                    }

                    PerformanceGrowthBlock(
                        modifier = Modifier.fillMaxWidth(),
                        metric = metric,
                        baseline = baseline,
                        nameLineHeight = nameLineHeight,
                        reveal = reveal,
                        entranceDelayMillis = index * GROWTH_REVEAL_STAGGER_MS,
                        onClick = { onMetricClick(metric.type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceGrowthBlock(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    baseline: PerformanceBaselineState,
    nameLineHeight: Dp,
    reveal: Transition<Boolean>,
    entranceDelayMillis: Int,
    onClick: () -> Unit,
) {
    val current = metric.chartValue()
    val average = metric.averageChartValue()
    val showsComparison = metric.showsComparison()
    // The baseline travels with the status, so a leaving chip keeps the words it was measured in.
    val statusChip = (metric.status to baseline).takeIf { showsComparison }
    val delta = metric.currentVsAveragePercentage?.takeIf { showsComparison }

    val mood = metric.indicatorColors()
    val muted = AppTokens.colors.charts.indicator.muted

    val startValue = if (current == null) 0f else average ?: 0f
    val endValue = current ?: 0f
    val scale = maxOf(endValue, average ?: 0f)
    val startFraction = if (scale > 0f) startValue / scale else 0f
    val afterFraction = if (scale > 0f) endValue / scale else 0f
    val beforeFraction = if (average != null && scale > 0f) average / scale else 0f

    // The settled Transition keeps its false → true segment; only currentState != targetState means entering.
    val isEntering = reveal.currentState != reveal.targetState
    val animatedValue = reveal.animateFloat(
        transitionSpec = { growthSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = GROWTH_VALUE_LABEL,
    ) { isRevealed -> if (isRevealed) endValue else startValue }
    val animatedAfterFraction = reveal.animateFloat(
        transitionSpec = { growthSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = GROWTH_AFTER_LABEL,
    ) { isRevealed -> if (isRevealed) afterFraction else startFraction }
    val fillColor = reveal.animateColor(
        transitionSpec = { growthSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = GROWTH_FILL_LABEL,
    ) { isRevealed -> if (isRevealed) mood.dominant else muted.dominant }
    val trackColor = reveal.animateColor(
        transitionSpec = { growthSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = GROWTH_TRACK_LABEL,
    ) { isRevealed -> if (isRevealed) mood.track else muted.track }
    val animatedBeforeFraction = reveal.animateFloat(
        transitionSpec = { growthSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = GROWTH_BEFORE_LABEL,
    ) { beforeFraction }

    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Top)
                    .height(nameLineHeight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.metrics.performance.trend.icon),
                    imageVector = metric.type.icon(),
                    tint = AppTokens.colors.icon.secondary,
                    contentDescription = null,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.type.label(),
                    style = AppTokens.typography.b13Semi(),
                    color = AppTokens.colors.text.primary,
                )
                GrowthOptionalPart(value = metric.takeIf { average != null }) { shown ->
                    GrowthCrossfade(
                        targetState = AppTokens.strings.res(
                            Res.string.performance_growth_before,
                            shown.averageDisplay(),
                        ),
                    ) { beforeText ->
                        Text(
                            text = beforeText,
                            style = AppTokens.typography.b11Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
                GrowthOptionalPart(value = statusChip) { shownChip ->
                    GrowthCrossfade(targetState = shownChip) { (status, statusBaseline) ->
                        PerformanceTrendChip(status = status, baseline = statusBaseline)
                    }
                }
            }

            if (!metric.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    GrowthValue(metric = metric, value = animatedValue)
                    GrowthOptionalPart(value = delta) { shownDelta ->
                        GrowthCrossfade(
                            targetState = PercentageFormatState.shortDelta(shownDelta),
                            contentAlignment = Alignment.TopEnd,
                        ) { deltaText ->
                            Text(
                                text = deltaText,
                                style = AppTokens.typography.b12Semi(),
                                color = AppTokens.colors.text.secondary,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }

            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
                imageVector = AppTokens.icons.ArrowRight,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null,
            )
        }

        GrowthOptionalPart(
            value = animatedBeforeFraction.takeIf { average != null && !metric.isEmpty() },
            padding = PaddingValues(top = AppTokens.dp.contentPadding.subContent),
        ) { shownBeforeFraction ->
            GrowthBars(
                modifier = Modifier.fillMaxWidth(),
                afterFraction = animatedAfterFraction,
                fillColor = fillColor,
                trackColor = trackColor,
                beforeFraction = shownBeforeFraction,
            )
        }
    }
}

private fun <T> growthSpec(isEntering: Boolean, entranceDelayMillis: Int): FiniteAnimationSpec<T> =
    if (isEntering) {
        tween(
            durationMillis = GROWTH_REVEAL_DURATION_MS,
            delayMillis = entranceDelayMillis,
            easing = FastOutSlowInEasing,
        )
    } else {
        growthSwitchTween()
    }

private fun <T> growthSwitchTween(): FiniteAnimationSpec<T> = tween(
    durationMillis = GROWTH_SWITCH_DURATION_MS,
    easing = FastOutSlowInEasing,
)

private val growthPartEnter: EnterTransition = expandVertically(
    animationSpec = growthSwitchTween(),
    expandFrom = Alignment.Top,
) + fadeIn(animationSpec = growthSwitchTween())

private val growthPartExit: ExitTransition = shrinkVertically(
    animationSpec = growthSwitchTween(),
    shrinkTowards = Alignment.Top,
) + fadeOut(animationSpec = growthSwitchTween())

@Composable
private fun <T : Any> GrowthOptionalPart(
    value: T?,
    padding: PaddingValues = PaddingValues(top = AppTokens.dp.contentPadding.text),
    content: @Composable (T) -> Unit,
) {
    var shown by remember { mutableStateOf(value) }
    if (value != null) {
        shown = value
    }

    AnimatedVisibility(
        visible = value != null,
        enter = growthPartEnter,
        exit = growthPartExit,
    ) {
        shown?.let { part ->
            Box(modifier = Modifier.padding(padding)) {
                content(part)
            }
        }
    }
}

@Composable
private fun <T> GrowthCrossfade(
    targetState: T,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn(animationSpec = growthSwitchTween()) togetherWith fadeOut(animationSpec = growthSwitchTween()))
                .using(sizeTransform = null)
        },
        contentAlignment = contentAlignment,
        label = GROWTH_CROSSFADE_LABEL,
    ) { state ->
        content(state)
    }
}

@Composable
private fun GrowthValue(
    metric: PerformanceMetricState,
    value: State<Float>,
) {
    Text(
        text = metric.displayValue(value.value),
        style = AppTokens.typography.h5(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun GrowthBars(
    modifier: Modifier = Modifier,
    afterFraction: State<Float>,
    fillColor: State<Color>,
    trackColor: State<Color>,
    beforeFraction: State<Float>,
) {
    val before = AppTokens.colors.charts.indicator.before

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        GrowthBar(
            fraction = { beforeFraction.value },
            fill = { before.dominant },
            track = { before.track },
        )
        GrowthBar(
            fraction = { afterFraction.value },
            fill = { fillColor.value },
            track = { trackColor.value },
        )
    }
}

@Composable
private fun GrowthBar(
    fraction: () -> Float,
    fill: () -> Color,
    track: () -> Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTokens.dp.metrics.charts.indicator.height)
            .drawBehind {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                val fillWidth = size.width * fraction().coerceIn(0f, 1f)

                drawRoundRect(color = track(), cornerRadius = corner)
                if (fillWidth > 0f) {
                    drawRoundRect(
                        color = fill(),
                        size = Size(maxOf(fillWidth, size.height).coerceAtMost(size.width), size.height),
                        cornerRadius = corner,
                    )
                }
            },
    )
}

@Composable
public fun PerformanceComparisonCardSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val nameLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val beforeLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val chipLine = rememberTextHeight(style = AppTokens.typography.b11Semi(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val deltaLine = rememberTextHeight(style = AppTokens.typography.b12Semi(), textMeasurer = textMeasurer)

    val textGap = AppTokens.dp.contentPadding.text
    val labelColumnHeight = nameLine + textGap + beforeLine
    val chipHeight = chipLine + AppTokens.dp.metrics.status.verticalPadding * 2
    val barsHeight = AppTokens.dp.metrics.charts.indicator.height * 2 + textGap
    val blockFrameHeight = AppTokens.dp.contentPadding.subContent * 3
    val blockHeight = maxOf(
        labelColumnHeight + textGap + chipHeight,
        valueLine + textGap + deltaLine,
        AppTokens.dp.metrics.overview.detailsIcon,
    ) + barsHeight + blockFrameHeight
    val panelSpacer = AppTokens.dp.metrics.panel.large.spacer

    SkeletonPanel(
        modifier = modifier,
        contentHeight = titleLine + panelSpacer + AppTokens.dp.segment.outline.height + panelSpacer +
            blockHeight * comparisonDisplayOrder.size +
            AppTokens.dp.divider.thickness * (comparisonDisplayOrder.size - 1),
        style = SkeletonPanelStyle.Large,
    )
}

@AppPreview
@Composable
private fun PerformanceComparisonCardSkeletonPreview() {
    PreviewContainer {
        PerformanceComparisonCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

private fun previewMixedMetrics(): ImmutableList<PerformanceMetricState> {
    val stubs = stubPerformanceMetrics()
    val declinedIntensity = stubs.filterIsInstance<PerformanceMetricState.Intensity>().first().copy(
        currentVsAveragePercentage = -8.0,
        current = 32f,
        average = 35f,
        status = PerformanceTrendStatusState.Declined,
    )
    val stableDensity = stubs.filterIsInstance<PerformanceMetricState.Density>().first().copy(
        currentVsAveragePercentage = 0.0,
        current = 20f,
        average = 20f,
        status = PerformanceTrendStatusState.Stable,
    )
    return persistentListOf(
        stableDensity,
        declinedIntensity,
        stubs.filterIsInstance<PerformanceMetricState.Volume>().first(),
        stubs.filterIsInstance<PerformanceMetricState.Repetitions>().first(),
    )
}

private fun previewPreviousMetrics(): ImmutableList<PerformanceMetricState> =
    previewMixedMetrics().map { metric ->
        when (metric) {
            is PerformanceMetricState.Volume -> metric.copy(status = PerformanceTrendStatusState.Improved)

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = null,
                average = null,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Duration,
            is PerformanceMetricState.Repetitions,
            is PerformanceMetricState.Intensity -> metric
        }
    }.toPersistentList()

@AppPreview
@Composable
private fun PerformanceComparisonCardPreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewMixedMetrics(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonCardPreviousBaselinePreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewPreviousMetrics(),
            baseline = PerformanceBaselineState.Previous,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

private fun previewFarBelowBestMetrics(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Volume -> metric.copy(
                currentVsAveragePercentage = 7.0,
                current = 1_070f,
                average = 1_000f,
                best = 3_400f,
                status = PerformanceTrendStatusState.Improved,
            )

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = 2.0,
                current = 20.4f,
                average = 20f,
                best = 41f,
                status = PerformanceTrendStatusState.Stable,
            )

            is PerformanceMetricState.Duration -> metric

            is PerformanceMetricState.Repetitions -> metric.copy(
                currentVsAveragePercentage = 8.0,
                current = 81,
                average = 75.0,
                best = 190,
                status = PerformanceTrendStatusState.Improved,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = -8.9,
                current = 31.9f,
                average = 35f,
                best = 70f,
                status = PerformanceTrendStatusState.Declined,
            )
        }
    }.toPersistentList()

@AppPreview
@Composable
private fun PerformanceComparisonCardFarBelowBestPreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewFarBelowBestMetrics(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

private fun previewZeroBaselineMetrics(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetricsByType().map { metric ->
        when (metric) {
            is PerformanceMetricState.Volume -> metric.copy(
                currentVsAveragePercentage = null,
                average = 0f,
                best = metric.current,
                status = PerformanceTrendStatusState.Record,
            )

            is PerformanceMetricState.Density -> metric.copy(
                currentVsAveragePercentage = null,
                average = 0f,
                best = metric.current,
                status = PerformanceTrendStatusState.Record,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                currentVsAveragePercentage = null,
                average = 0f,
                best = metric.current,
                status = PerformanceTrendStatusState.Record,
            )

            is PerformanceMetricState.Duration,
            is PerformanceMetricState.Repetitions -> metric
        }
    }.toPersistentList()

@AppPreview
@Composable
private fun PerformanceComparisonCardZeroBaselinePreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewZeroBaselineMetrics(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonCardFirstWorkoutPreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = stubPerformanceMetricsWithoutBaseline(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonCardEmptyPreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = stubEmptyPerformanceMetrics(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonCardEmptyMetricPreview() {
    PreviewContainer {
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = stubPerformanceMetricsByType().map { metric ->
                if (metric is PerformanceMetricState.Density) {
                    metric.copy(
                        currentVsAveragePercentage = null,
                        current = null,
                        status = PerformanceTrendStatusState.Empty,
                    )
                } else {
                    metric
                }
            }.toPersistentList(),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonCardLargeAndFractionalPreview() {
    PreviewContainer {
        val stubs = stubPerformanceMetrics()
        val volume = stubs.filterIsInstance<PerformanceMetricState.Volume>().first()
        val repetitions = stubs.filterIsInstance<PerformanceMetricState.Repetitions>().first()
        val emptyDensity = stubEmptyPerformanceMetrics().filterIsInstance<PerformanceMetricState.Density>().first()
        PerformanceComparisonCard(
            modifier = Modifier.fillMaxWidth(),
            metrics = persistentListOf(
                volume.copy(
                    current = 123_456.7f,
                    average = 100_000f,
                    best = 123_456.7f,
                    currentVsAveragePercentage = 23.4567,
                    status = PerformanceTrendStatusState.Record,
                ),
                repetitions.copy(
                    current = 1_084,
                    average = 975.5,
                    best = 1_084,
                    currentVsAveragePercentage = 11.2583,
                ),
                emptyDensity,
            ),
            baseline = PerformanceBaselineState.Average,
            onBaselineSelect = {},
            onMetricClick = {},
        )
    }
}
