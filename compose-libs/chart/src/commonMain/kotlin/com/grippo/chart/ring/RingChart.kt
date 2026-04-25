package com.grippo.chart.ring

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

private const val RING_ANIMATION_DURATION_MS = 500
private const val RING_PROGRESS_ANIMATION_LABEL = "RingChartProgress"
private const val RING_INDICATOR_COLOR_ANIMATION_LABEL = "RingChartIndicatorColor"
private const val RING_TRACK_COLOR_ANIMATION_LABEL = "RingChartTrackColor"

@Composable
public fun RingChart(
    modifier: Modifier = Modifier,
    data: RingData,
    style: RingStyle,
) {
    val domain = (data.max - data.min).takeIf { it > 0f } ?: 1f
    val rawProgress = (data.value - data.min) / domain
    val targetProgress = if (rawProgress.isNaN()) 0f else rawProgress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = RING_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = RING_PROGRESS_ANIMATION_LABEL,
    )
    val animatedIndicatorColor by animateColorAsState(
        targetValue = style.indicatorColor,
        animationSpec = tween(
            durationMillis = RING_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = RING_INDICATOR_COLOR_ANIMATION_LABEL,
    )
    val animatedTrackColor by animateColorAsState(
        targetValue = style.trackColor,
        animationSpec = tween(
            durationMillis = RING_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = RING_TRACK_COLOR_ANIMATION_LABEL,
    )

    Spacer(
        modifier = modifier
            .drawWithCache {
                // Geometry and Stroke are built in drawWithCache so they're
                // computed once per size/style change rather than on every
                // animation frame. Animated values (progress, colors) are
                // read inside onDrawBehind via state reads, so they only
                // trigger redraw — never re-running this build block.
                val stroke = style.strokeWidth.toPx().coerceAtLeast(1f)
                val diameter = size.minDimension - stroke
                val offset = (size.minDimension - diameter) / 2f
                val topLeft = Offset(offset, offset)
                val arcSize = Size(diameter, diameter)
                val strokeStyle = Stroke(width = stroke, cap = style.cap)
                val startAngle = style.startAngle
                val sweepAngle = style.sweepAngle

                onDrawBehind {
                    if (diameter <= 0f) return@onDrawBehind

                    drawArc(
                        color = animatedTrackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = strokeStyle,
                    )

                    val progressSweep = sweepAngle * animatedProgress
                    if (progressSweep != 0f) {
                        drawArc(
                            color = animatedIndicatorColor,
                            startAngle = startAngle,
                            sweepAngle = progressSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = strokeStyle,
                        )
                    }
                }
            }
    )
}
