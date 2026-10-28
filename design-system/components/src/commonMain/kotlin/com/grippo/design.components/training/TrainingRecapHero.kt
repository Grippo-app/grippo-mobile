package com.grippo.design.components.training

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceVerdictState
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsDeclined
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsImproved
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutReference
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubRecapTraining
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_no_sets
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.listed_pair
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.show_more
import com.grippo.design.resources.provider.training_recap_sets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val RECAP_FACT_COUNT = 3
private const val RECAP_BACKDROP_REVEAL_MS = 900
private const val RECAP_BACKDROP_SWITCH_MS = 250
private const val RECAP_BACKDROP_REVEAL_LABEL = "RecapBackdropReveal"
private const val RECAP_BACKDROP_SWITCH_LABEL = "RecapBackdropSwitch"

private const val RECAP_COLLAPSE_PARALLAX = 0.5f
private const val RECAP_COLLAPSE_TITLE_SCALE = 0.86f
private const val RECAP_COLLAPSE_FACTS_FADE_END = 0.35f
private const val RECAP_COLLAPSE_CROSSFADE_START = 0.4f
private const val RECAP_COLLAPSE_CROSSFADE_END = 0.6f
private const val RECAP_COLLAPSE_SEMANTICS_SWITCH = 0.5f
private const val RECAP_MAX_HEIGHT_SHARE = 0.5f

@Immutable
public sealed interface TrainingRecapHeroStyle {
    @Immutable
    public data object Pending : TrainingRecapHeroStyle

    @Immutable
    public data class Resolved(
        public val verdict: PerformanceVerdictState,
        public val reference: PerformanceReferenceState,
        public val description: String,
    ) : TrainingRecapHeroStyle
}

private class CollapseInterruptedException : CancellationException()

@Stable
public class TrainingRecapHeroState internal constructor(
    revealed: Boolean,
    initialCollapseOffset: Float,
) {
    internal val backdropReveal: Animatable<Float, AnimationVector1D> = Animatable(
        initialValue = if (revealed) 1f else 0f,
        typeConverter = Float.VectorConverter,
        label = RECAP_BACKDROP_REVEAL_LABEL,
    )
    internal var backdropGeneration: Int by mutableIntStateOf(0)
        private set

    internal var collapseOffset: Float by mutableFloatStateOf(initialCollapseOffset)
        private set

    // Written by the measure pass: scrolling and the semantics switch need the laid-out bounds.
    internal var collapseFloor: Float by mutableFloatStateOf(0f)
    internal var collapseRange: Float by mutableFloatStateOf(0f)
    private var collapseGeneration: Int = 0
    internal val collapseFraction: Float
        get() = if (collapseRange > 0f) {
            (collapseOffset.coerceIn(collapseFloor, collapseRange) / collapseRange).coerceIn(0f, 1f)
        } else {
            0f
        }

    public val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            return Offset(x = 0f, y = -collapseBy(-available.y))
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (available.y <= 0f) return Offset.Zero
            return Offset(x = 0f, y = -collapseBy(-available.y))
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settleCollapse()
            return Velocity.Zero
        }
    }

    public suspend fun whileExpanded(block: suspend () -> Unit) {
        expand()
        val expandedOffset = collapseOffset
        coroutineScope {
            launch {
                snapshotFlow { collapseOffset }.first { offset -> offset != expandedOffset }
                this@coroutineScope.cancel()
            }
            block()
            coroutineContext.cancelChildren()
        }
    }

    internal suspend fun expand() {
        animateCollapse(target = collapseFloor)
    }

    public suspend fun settle() {
        backdropReveal.snapTo(targetValue = 1f)
        backdropGeneration++
        // With one frame a caller would still read the backdrop drawn before it settled.
        withFrameNanos { }
        withFrameNanos { }
    }

    internal suspend fun reveal() {
        if (backdropReveal.value == 1f) return

        backdropReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = RECAP_BACKDROP_REVEAL_MS,
                easing = FastOutSlowInEasing
            ),
        )
    }

    internal fun collapseBy(delta: Float): Float {
        collapseGeneration++
        val previous = collapseOffset.coerceIn(collapseFloor, collapseRange)
        collapseOffset = (previous + delta).coerceIn(collapseFloor, collapseRange)
        return collapseOffset - previous
    }

    internal suspend fun settleCollapse() {
        val offset = collapseOffset
        if (offset <= collapseFloor || offset >= collapseRange) return

        val isNearerExpanded = offset - collapseFloor < (collapseRange - collapseFloor) / 2f
        try {
            animateCollapse(target = if (isNearerExpanded) collapseFloor else collapseRange)
        } catch (_: CollapseInterruptedException) {
        }
    }

    private suspend fun animateCollapse(target: Float) {
        val generation = ++collapseGeneration
        val start = collapseOffset.coerceIn(collapseFloor, collapseRange)
        if (start == target) {
            collapseOffset = target
            return
        }

        animate(
            initialValue = start,
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) { value, _ ->
            if (generation != collapseGeneration) throw CollapseInterruptedException()
            collapseOffset = value
        }
    }

    internal companion object {
        val Saver: Saver<TrainingRecapHeroState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.backdropReveal.targetValue == 1f,
                    state.collapseOffset
                )
            },
            restore = { saved ->
                TrainingRecapHeroState(
                    revealed = saved[0] as Boolean,
                    initialCollapseOffset = saved[1] as Float
                )
            },
        )
    }
}

@Composable
public fun rememberTrainingRecapHeroState(): TrainingRecapHeroState {
    return rememberSaveable(saver = TrainingRecapHeroState.Saver) {
        TrainingRecapHeroState(revealed = false, initialCollapseOffset = 0f)
    }
}

@Composable
public fun TrainingRecapHero(
    modifier: Modifier = Modifier,
    training: TrainingState,
    style: TrainingRecapHeroStyle,
    state: TrainingRecapHeroState = rememberTrainingRecapHeroState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val durationValue = training.duration.value
    val volumeValue = training.total.volume.value
    val setsCount = training.exercises.sumOf { exercise -> exercise.iterations.size }
    val resolvedVerdict = (style as? TrainingRecapHeroStyle.Resolved)?.verdict
    val textMeasurer = rememberTextMeasurer()
    val compactFacts = listOfNotNull(
        durationValue?.let { value -> DurationFormatState.shortMinutes(value) },
        volumeValue?.let { value -> VolumeFormatState.shortTotal(value) },
        if (setsCount == 0) {
            AppTokens.strings.res(Res.string.exercise_history_no_sets)
        } else {
            AppTokens.strings.res(Res.plurals.training_recap_sets, setsCount, setsCount)
        },
    ).reduce { line, fact -> AppTokens.strings.res(Res.string.joined_pair, line, fact) }
    val isCollapsed by remember(state) {
        derivedStateOf { state.collapseFraction >= RECAP_COLLAPSE_SEMANTICS_SWITCH }
    }
    val isExpandable by remember(state) {
        derivedStateOf { state.collapseFloor < state.collapseRange }
    }
    val expandLabel = AppTokens.strings.res(Res.string.show_more)
    val coroutineScope = rememberCoroutineScope()
    val expandedSemantics = if (isCollapsed) Modifier.clearAndSetSemantics { } else Modifier
    val barSemantics = when {
        !isCollapsed -> Modifier.clearAndSetSemantics { }
        isExpandable -> Modifier.semantics(mergeDescendants = true) {
            customActions = listOf(
                CustomAccessibilityAction(label = expandLabel) {
                    coroutineScope.launch { state.expand() }
                    true
                },
            )
        }

        else -> Modifier.semantics(mergeDescendants = true) { }
    }

    RecapCollapsingSurface(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        text = {
            Column(
                modifier = expandedSemantics,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                when (style) {
                    TrainingRecapHeroStyle.Pending -> {
                        RecapTitleSkeleton(textMeasurer = textMeasurer)
                        RecapDescriptionSkeleton(textMeasurer = textMeasurer)
                    }

                    is TrainingRecapHeroStyle.Resolved -> {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { heading() },
                            text = style.verdict.title(reference = style.reference),
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
        },
        backdrop = {
            RecapBackdrop(
                verdict = resolvedVerdict,
                state = state,
            )
        },
        facts = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(expandedSemantics),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
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
                if (setsCount > 0) {
                    RecapFact(
                        modifier = Modifier.weight(1f),
                        value = setsCount.toString(),
                        label = AppTokens.strings.res(Res.string.sets),
                    )
                }
            }
        },
        bar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(barSemantics),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                when (style) {
                    TrainingRecapHeroStyle.Pending -> RecapBarTitleSkeleton(textMeasurer = textMeasurer)

                    is TrainingRecapHeroStyle.Resolved -> Text(
                        modifier = Modifier.semantics { heading() },
                        text = style.verdict.title(reference = style.reference),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = compactFacts,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
public fun TrainingRecapHeroSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val textMeasurer = rememberTextMeasurer()
    val state = remember { TrainingRecapHeroState(revealed = true, initialCollapseOffset = 0f) }

    RecapCollapsingSurface(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        isDraggable = false,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
                RecapTitleSkeleton(textMeasurer = textMeasurer)
                RecapDescriptionSkeleton(textMeasurer = textMeasurer)
            }
        },
        backdrop = {
            Spacer(modifier = Modifier.size(AppTokens.dp.illustration.compact))
        },
        facts = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                repeat(RECAP_FACT_COUNT) {
                    RecapFactSkeleton(modifier = Modifier.weight(1f), textMeasurer = textMeasurer)
                }
            }
        },
        bar = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                RecapBarTitleSkeleton(textMeasurer = textMeasurer)
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                    style = AppTokens.typography.b12Med(),
                    textMeasurer = textMeasurer,
                )
            }
        },
    )
}

@Composable
private fun RecapCollapsingSurface(
    modifier: Modifier = Modifier,
    state: TrainingRecapHeroState,
    contentPadding: PaddingValues,
    isDraggable: Boolean = true,
    text: @Composable () -> Unit,
    backdrop: @Composable () -> Unit,
    facts: @Composable () -> Unit,
    bar: @Composable () -> Unit,
) {
    val horizontalPadding = AppTokens.dp.screen.horizontalPadding + AppTokens.dp.brandHeader.horizontalPadding
    val expandedPadding = AppTokens.dp.brandHeader.verticalPadding
    val rowGap = AppTokens.dp.contentPadding.content
    val backdropSize = AppTokens.dp.illustration.compact
    val collapsed = AppTokens.dp.training.recap.collapsed
    val dragState = rememberDraggableState { delta -> state.collapseBy(-delta) }

    LaunchedEffect(state) {
        snapshotFlow { state.collapseFloor to state.collapseRange }.collect { state.settleCollapse() }
    }

    Layout(
        contents = listOf(
            { Box(modifier = Modifier.illustrationBackground(RectangleShape)) },
            text,
            backdrop,
            facts,
            bar,
        ),
        modifier = modifier
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                enabled = isDraggable,
                onDragStopped = { state.settleCollapse() },
            ),
    ) { (surfaceMeasurables, textMeasurables, backdropMeasurables, factsMeasurables, barMeasurables), constraints ->
        val startPadding =
            (contentPadding.calculateStartPadding(layoutDirection) + horizontalPadding).roundToPx()
        val endPadding =
            (contentPadding.calculateEndPadding(layoutDirection) + horizontalPadding).roundToPx()
        val topInset = contentPadding.calculateTopPadding().roundToPx()
        val bottomInset = contentPadding.calculateBottomPadding().roundToPx()
        val expandedPaddingPx = expandedPadding.roundToPx()
        val collapsedPaddingPx = collapsed.verticalPadding.roundToPx()
        val rowGapPx = rowGap.roundToPx()
        val backdropPx = backdropSize.roundToPx()
        val collapsedBackdropPx = collapsed.backdrop.roundToPx()

        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val contentWidth = (width - startPadding - endPadding).coerceAtLeast(0)
        val textPlaceable = textMeasurables.first().measure(
            Constraints(maxWidth = (contentWidth - backdropPx - rowGapPx).coerceAtLeast(0)),
        )
        val backdropPlaceable =
            backdropMeasurables.first().measure(Constraints.fixed(backdropPx, backdropPx))
        val factsPlaceable = factsMeasurables.first().measure(Constraints(maxWidth = contentWidth))
        val barPlaceable = barMeasurables.first().measure(
            Constraints(maxWidth = (contentWidth - collapsedBackdropPx - rowGapPx).coerceAtLeast(0)),
        )

        val topRowHeight = maxOf(textPlaceable.height, backdropPx)
        val factsGapPx = if (factsPlaceable.height > 0) rowGapPx else 0
        val expandedHeight = topInset + expandedPaddingPx + topRowHeight + factsGapPx +
                factsPlaceable.height + expandedPaddingPx + bottomInset
        val barRowHeight = maxOf(barPlaceable.height, collapsedBackdropPx)
        val collapsedHeight =
            topInset + collapsedPaddingPx + barRowHeight + collapsedPaddingPx + bottomInset
        val range = (expandedHeight - collapsedHeight).coerceAtLeast(0).toFloat()
        val heightLimit = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight * RECAP_MAX_HEIGHT_SHARE).roundToInt()
        } else {
            expandedHeight
        }
        val floor = if (expandedHeight > heightLimit) range else 0f
        state.collapseRange = range
        state.collapseFloor = floor

        val offset = state.collapseOffset.coerceIn(floor, range)
        val fraction = if (range > 0f) offset / range else 0f
        val height = (expandedHeight - offset.roundToInt()).coerceIn(
            constraints.minHeight,
            constraints.maxHeight
        )
        val surfacePlaceable = surfaceMeasurables.first().measure(Constraints.fixed(width, height))

        val expandedTop = topInset + expandedPaddingPx
        val collapsedTop = topInset + collapsedPaddingPx
        val surfaceRadius = collapsed.radius.toPx() * fraction
        val parallax = (offset * RECAP_COLLAPSE_PARALLAX).roundToInt()
        val titleScale = lerp(1f, RECAP_COLLAPSE_TITLE_SCALE, fraction)
        val titleOrigin =
            TransformOrigin(if (layoutDirection == LayoutDirection.Ltr) 0f else 1f, 0f)
        val backdropScale = lerp(1f, collapsedBackdropPx / backdropPx.toFloat(), fraction)
        val backdropInset = backdropPx * (1f - backdropScale) / 2f
        val backdropX = lerp(
            start = (width - endPadding - backdropPx).toFloat(),
            stop = (width - endPadding - collapsedBackdropPx).toFloat(),
            fraction = fraction,
        )
        val backdropY = lerp(
            start = expandedTop.toFloat(),
            stop = (collapsedTop + (barRowHeight - collapsedBackdropPx) / 2).toFloat(),
            fraction = fraction,
        )
        val barAlpha = collapsePhase(
            fraction = fraction,
            start = RECAP_COLLAPSE_CROSSFADE_START,
            end = RECAP_COLLAPSE_CROSSFADE_END,
        )
        val barRise = collapsed.rise.toPx()

        layout(width, height) {
            surfacePlaceable.placeWithLayer(x = 0, y = 0) {
                shape = RoundedCornerShape(bottomStart = surfaceRadius, bottomEnd = surfaceRadius)
                clip = true
            }
            textPlaceable.placeRelativeWithLayer(x = startPadding, y = expandedTop - parallax) {
                alpha = 1f - barAlpha
                scaleX = titleScale
                scaleY = titleScale
                transformOrigin = titleOrigin
            }
            factsPlaceable.placeRelativeWithLayer(
                x = startPadding,
                y = expandedTop + topRowHeight + factsGapPx - parallax,
            ) {
                alpha = 1f - collapsePhase(
                    fraction = fraction,
                    start = 0f,
                    end = RECAP_COLLAPSE_FACTS_FADE_END
                )
            }
            backdropPlaceable.placeRelativeWithLayer(
                x = (backdropX - backdropInset).roundToInt(),
                y = (backdropY - backdropInset).roundToInt(),
            ) {
                scaleX = backdropScale
                scaleY = backdropScale
            }
            barPlaceable.placeRelativeWithLayer(
                x = startPadding,
                y = collapsedTop + (barRowHeight - barPlaceable.height) / 2,
            ) {
                alpha = barAlpha
                translationY = (1f - barAlpha) * barRise
            }
        }
    }
}

private fun collapsePhase(fraction: Float, start: Float, end: Float): Float {
    return ((fraction - start) / (end - start)).coerceIn(0f, 1f)
}

@Composable
private fun RecapBackdrop(
    verdict: PerformanceVerdictState?,
    state: TrainingRecapHeroState,
) {
    Box(modifier = Modifier.size(AppTokens.dp.illustration.compact)) {
        if (verdict != null) {
            LaunchedEffect(state) { state.reveal() }
            val rise =
                with(LocalDensity.current) { AppTokens.dp.training.recap.backdrop.rise.toPx() }

            key(state.backdropGeneration) {
                Crossfade(
                    modifier = Modifier.graphicsLayer {
                        val reveal = state.backdropReveal.value
                        alpha = reveal
                        translationY = (1f - reveal) * rise
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
private fun RecapBarTitleSkeleton(textMeasurer: TextMeasurer) {
    SkeletonText(
        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
        style = AppTokens.typography.h4(),
        textMeasurer = textMeasurer,
    )
}

@Composable
private fun RecapFact(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    val description = AppTokens.strings.res(Res.string.listed_pair, label, value)

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
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
    reference: PerformanceReferenceState = PerformanceReferenceState.Average,
): TrainingRecapHeroStyle.Resolved {
    val verdict = PerformanceVerdictState.of(metrics)
    return TrainingRecapHeroStyle.Resolved(
        verdict = verdict,
        reference = reference,
        description = verdict.summary(metrics = metrics, reference = reference),
    )
}

@AppPreview
@Composable
private fun TrainingRecapHeroRecordPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubRecapTraining(),
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
            training = stubRecapTraining(),
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
            training = stubRecapTraining(),
            style = TrainingRecapHeroStyle.Resolved(
                verdict = PerformanceVerdictState.Stable,
                reference = PerformanceReferenceState.Average,
                description = PerformanceReferenceState.Average.comparedSummary(
                    improved = 2,
                    compared = 4
                ),
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
            training = stubRecapTraining(),
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
            training = stubRecapTraining(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsWithoutReference()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroPendingPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubRecapTraining(),
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
private fun TrainingRecapHeroPreviousReferencePreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubRecapTraining(),
            style = previewResolvedStyle(
                metrics = stubPerformanceMetricsImproved(),
                reference = PerformanceReferenceState.Previous,
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroLongValuesPreview() {
    val training = stubRecapTraining()
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = training.copy(
                duration = DurationFormatState.ofTotal(23.hours + 59.minutes),
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
    val training = stubRecapTraining()
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = training.copy(
                duration = DurationFormatState.Empty(),
                exercises = persistentListOf(),
                total = training.total.copy(volume = VolumeFormatState.Empty()),
            ),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsWithoutReference()),
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroCollapsedPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubRecapTraining(),
            style = previewResolvedStyle(metrics = stubPerformanceMetricsImproved()),
            state = remember {
                TrainingRecapHeroState(
                    revealed = true,
                    initialCollapseOffset = Float.MAX_VALUE
                )
            },
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroCollapsedPendingPreview() {
    PreviewContainer {
        TrainingRecapHero(
            modifier = Modifier.fillMaxWidth(),
            training = stubRecapTraining(),
            style = TrainingRecapHeroStyle.Pending,
            state = remember {
                TrainingRecapHeroState(
                    revealed = true,
                    initialCollapseOffset = Float.MAX_VALUE
                )
            },
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecapHeroSkeletonCollapsedPreview() {
    PreviewContainer {
        TrainingRecapHeroSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
        )
    }
}
