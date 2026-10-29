package com.grippo.design.components.metrics.performance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetrics
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutReference
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.empty.EmptyStateStyle
import com.grippo.design.components.indicators.ComparisonIndicator
import com.grippo.design.components.metrics.performance.internal.PerformanceTrendChip
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.ValueSlot
import com.grippo.design.components.utils.buildValueSlotString
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.performance_comparison_after
import com.grippo.design.resources.provider.performance_comparison_before
import com.grippo.design.resources.provider.performance_comparison_empty
import com.grippo.design.resources.provider.performance_comparison_first_title
import com.grippo.design.resources.provider.value_transition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val comparisonDisplayOrder: ImmutableList<PerformanceMetricTypeState> = persistentListOf(
    PerformanceMetricTypeState.Volume,
    PerformanceMetricTypeState.Repetitions,
    PerformanceMetricTypeState.Intensity,
    PerformanceMetricTypeState.Density,
)

private const val COMPARISON_REVEAL_DURATION_MS = 900
private const val COMPARISON_REVEAL_STAGGER_MS = 120
private const val COMPARISON_SWITCH_DURATION_MS = 250
private const val COMPARISON_REVEAL_LABEL = "PerformanceComparisonReveal"
private const val COMPARISON_VALUE_LABEL = "PerformanceComparisonValue"
private const val COMPARISON_AFTER_LABEL = "PerformanceComparisonAfter"
private const val COMPARISON_BEFORE_LABEL = "PerformanceComparisonBefore"
private const val COMPARISON_CROSSFADE_LABEL = "PerformanceComparisonCrossfade"

@Composable
public fun PerformanceComparisonSection(
    modifier: Modifier = Modifier,
    metrics: ImmutableList<PerformanceMetricState>,
    reference: PerformanceReferenceState,
    onReferenceSelect: (PerformanceReferenceState) -> Unit,
    onMetricClick: (PerformanceMetricTypeState) -> Unit,
) {
    val orderedMetrics = remember(metrics) {
        comparisonDisplayOrder.mapNotNull { type -> metrics.firstOrNull { it.type == type } }
    }
    val hasValues = orderedMetrics.any { !it.isEmpty() }
    val hasReference = orderedMetrics.any { it.hasAverage() }
    val referenceItems = remember {
        PerformanceReferenceState.entries.map { entry -> entry to entry.labelText() }.toPersistentList()
    }
    val nameLineHeight = rememberTextHeight(style = AppTokens.typography.b13Semi())

    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal = updateTransition(targetState = revealed, label = COMPARISON_REVEAL_LABEL)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.section.spacer),
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = if (hasValues && !hasReference) {
                AnnotatedString(AppTokens.strings.res(Res.string.performance_comparison_first_title))
            } else {
                comparisonTitle()
            },
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        if (!hasValues) {
            EmptyState(
                text = AppTokens.strings.res(Res.string.performance_comparison_empty),
                style = EmptyStateStyle.Text,
            )
            return@Column
        }

        if (hasReference) {
            Segment(
                modifier = Modifier.fillMaxWidth(),
                items = referenceItems,
                style = SegmentStyle.Outline,
                selected = reference,
                onSelect = onReferenceSelect,
                segmentWidth = SegmentWidth.EqualFill,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            orderedMetrics.forEachIndexed { index, metric ->
                key(metric.type) {
                    if (index > 0) {
                        ContentDivider(
                            modifier = Modifier.padding(
                                start = AppTokens.dp.metrics.performance.trend.icon +
                                    AppTokens.dp.contentPadding.subContent,
                            ),
                        )
                    }

                    PerformanceComparisonRow(
                        modifier = Modifier.fillMaxWidth(),
                        metric = metric,
                        reference = reference,
                        nameLineHeight = nameLineHeight,
                        reveal = reveal,
                        entranceDelayMillis = index * COMPARISON_REVEAL_STAGGER_MS,
                        onClick = { onMetricClick(metric.type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun comparisonTitle(): AnnotatedString {
    val before = AppTokens.strings.res(Res.string.performance_comparison_before)
    val after = AppTokens.strings.res(Res.string.performance_comparison_after)
    val sentence = AppTokens.strings.res(Res.string.value_transition, before, ValueSlot.marker(0))
    val beforeStyle = SpanStyle(color = AppTokens.colors.text.secondary)

    return buildValueSlotString(
        sentence = sentence,
        slots = listOf(ValueSlot(value = after)),
        appendRest = { rest -> withStyle(beforeStyle) { append(rest) } },
    )
}

@Composable
private fun PerformanceComparisonRow(
    modifier: Modifier = Modifier,
    metric: PerformanceMetricState,
    reference: PerformanceReferenceState,
    nameLineHeight: Dp,
    reveal: Transition<Boolean>,
    entranceDelayMillis: Int,
    onClick: () -> Unit,
) {
    val current = metric.chartValue()
    val average = metric.averageChartValue()
    val showsComparison = metric.showsComparison()
    val hasComparisonIndicator = average != null && !metric.isEmpty()
    // The reference travels with the status, so a leaving chip keeps the words it was measured in.
    val statusChip = Triple(
        first = metric.status,
        second = reference,
        third = !hasComparisonIndicator,
    ).takeIf { showsComparison }
    val earlierLine = (metric to reference).takeIf { metric.hasEarlierValue() }
    val delta = metric.currentVsAveragePercentage?.takeIf { showsComparison }

    val indicatorColors = metric.indicatorColors()

    val startValue = if (current == null) 0f else average ?: 0f
    val endValue = current ?: 0f
    val scale = maxOf(endValue, average ?: 0f)
    val startFraction = if (scale > 0f) startValue / scale else 0f
    val afterFraction = if (scale > 0f) endValue / scale else 0f
    val beforeFraction = if (average != null && scale > 0f) average / scale else 0f

    // The settled Transition keeps its false → true segment; only currentState != targetState means entering.
    val isEntering = reveal.currentState != reveal.targetState
    val animatedValue = reveal.animateFloat(
        transitionSpec = { comparisonSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = COMPARISON_VALUE_LABEL,
    ) { isRevealed -> if (isRevealed) endValue else startValue }
    val animatedAfterFraction = reveal.animateFloat(
        transitionSpec = { comparisonSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = COMPARISON_AFTER_LABEL,
    ) { isRevealed -> if (isRevealed) afterFraction else startFraction }
    val animatedBeforeFraction = reveal.animateFloat(
        transitionSpec = { comparisonSpec(isEntering = isEntering, entranceDelayMillis = entranceDelayMillis) },
        label = COMPARISON_BEFORE_LABEL,
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
                ComparisonOptionalPart(value = earlierLine) { (shownMetric, shownReference) ->
                    ComparisonCrossfade(
                        targetState = shownReference.earlierValue(shownMetric.averageDisplay()),
                    ) { beforeText ->
                        Text(
                            text = beforeText,
                            style = AppTokens.typography.b11Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
                ComparisonOptionalPart(value = statusChip) { shownChip ->
                    ComparisonCrossfade(targetState = shownChip) { (status, statusReference, emphasized) ->
                        PerformanceTrendChip(status = status, reference = statusReference, emphasized = emphasized)
                    }
                }
            }

            if (!metric.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    ComparisonValue(metric = metric, value = animatedValue)
                    ComparisonOptionalPart(value = delta) { shownDelta ->
                        ComparisonCrossfade(
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

        ComparisonOptionalPart(
            value = animatedBeforeFraction.takeIf { hasComparisonIndicator },
            padding = PaddingValues(top = AppTokens.dp.contentPadding.subContent),
        ) { shownBeforeFraction ->
            ComparisonIndicator(
                modifier = Modifier.fillMaxWidth(),
                value = { animatedAfterFraction.value },
                reference = { shownBeforeFraction.value },
                colors = indicatorColors,
            )
        }
    }
}

private fun <T> comparisonSpec(isEntering: Boolean, entranceDelayMillis: Int): FiniteAnimationSpec<T> =
    if (isEntering) {
        tween(
            durationMillis = COMPARISON_REVEAL_DURATION_MS,
            delayMillis = entranceDelayMillis,
            easing = FastOutSlowInEasing,
        )
    } else {
        comparisonSwitchTween()
    }

private fun <T> comparisonSwitchTween(): FiniteAnimationSpec<T> = tween(
    durationMillis = COMPARISON_SWITCH_DURATION_MS,
    easing = FastOutSlowInEasing,
)

private val comparisonPartEnter: EnterTransition = expandVertically(
    animationSpec = comparisonSwitchTween(),
    expandFrom = Alignment.Top,
) + fadeIn(animationSpec = comparisonSwitchTween())

private val comparisonPartExit: ExitTransition = shrinkVertically(
    animationSpec = comparisonSwitchTween(),
    shrinkTowards = Alignment.Top,
) + fadeOut(animationSpec = comparisonSwitchTween())

@Composable
private fun <T : Any> ComparisonOptionalPart(
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
        enter = comparisonPartEnter,
        exit = comparisonPartExit,
    ) {
        shown?.let { part ->
            Box(modifier = Modifier.padding(padding)) {
                content(part)
            }
        }
    }
}

@Composable
private fun <T> ComparisonCrossfade(
    targetState: T,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn(animationSpec = comparisonSwitchTween()) togetherWith fadeOut(animationSpec = comparisonSwitchTween()))
                .using(sizeTransform = null)
        },
        contentAlignment = contentAlignment,
        label = COMPARISON_CROSSFADE_LABEL,
    ) { state ->
        content(state)
    }
}

@Composable
private fun ComparisonValue(
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
public fun PerformanceComparisonSectionSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val nameLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val beforeLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val chipLine = rememberTextHeight(style = AppTokens.typography.b11Semi(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val deltaLine = rememberTextHeight(style = AppTokens.typography.b12Semi(), textMeasurer = textMeasurer)

    val textGap = AppTokens.dp.contentPadding.text
    val labelColumnHeight = nameLine + textGap + beforeLine
    val chipHeight = chipLine + AppTokens.dp.metrics.status.verticalPadding * 2
    val indicatorHeight = maxOf(
        AppTokens.dp.metrics.charts.comparison.height,
        AppTokens.dp.metrics.charts.comparison.marker.height,
    )
    val blockFrameHeight = AppTokens.dp.contentPadding.subContent * 3
    val blockHeight = maxOf(
        labelColumnHeight + textGap + chipHeight,
        valueLine + textGap + deltaLine,
        AppTokens.dp.metrics.overview.detailsIcon,
    ) + indicatorHeight + blockFrameHeight
    val spacer = AppTokens.dp.metrics.section.spacer
    val contentHeight = AppTokens.dp.segment.outline.height + spacer +
        blockHeight * comparisonDisplayOrder.size +
        AppTokens.dp.divider.thickness * (comparisonDisplayOrder.size - 1)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacer),
    ) {
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
            style = AppTokens.typography.h4(),
            textMeasurer = textMeasurer,
        )
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionSkeletonPreview() {
    PreviewContainer {
        PerformanceComparisonSectionSkeleton(modifier = Modifier.fillMaxWidth())
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
private fun PerformanceComparisonSectionPreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewMixedMetrics(),
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionPreviousReferencePreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewPreviousMetrics(),
            reference = PerformanceReferenceState.Previous,
            onReferenceSelect = {},
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
private fun PerformanceComparisonSectionFarBelowBestPreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewFarBelowBestMetrics(),
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

private fun previewZeroReferenceMetrics(): ImmutableList<PerformanceMetricState> =
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
private fun PerformanceComparisonSectionZeroReferencePreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = previewZeroReferenceMetrics(),
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionFirstWorkoutPreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = stubPerformanceMetricsWithoutReference(),
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionEmptyPreview() {
    PreviewContainer {
        PerformanceComparisonSection(
            modifier = Modifier.fillMaxWidth(),
            metrics = stubEmptyPerformanceMetrics(),
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionEmptyMetricPreview() {
    PreviewContainer {
        PerformanceComparisonSection(
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
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceComparisonSectionLargeAndFractionalPreview() {
    PreviewContainer {
        val stubs = stubPerformanceMetrics()
        val volume = stubs.filterIsInstance<PerformanceMetricState.Volume>().first()
        val repetitions = stubs.filterIsInstance<PerformanceMetricState.Repetitions>().first()
        val emptyDensity = stubEmptyPerformanceMetrics().filterIsInstance<PerformanceMetricState.Density>().first()
        PerformanceComparisonSection(
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
            reference = PerformanceReferenceState.Average,
            onReferenceSelect = {},
            onMetricClick = {},
        )
    }
}
