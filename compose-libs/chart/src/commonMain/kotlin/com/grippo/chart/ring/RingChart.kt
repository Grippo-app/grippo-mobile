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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

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
    val domain = data.max.toDouble() - data.min.toDouble()
    val targetProgress = if (data.value.isFinite() && data.min.isFinite() && domain.isFinite() && domain > 0.0) {
        ((data.value.toDouble() - data.min) / domain).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

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
                val stroke = style.strokeWidth.toPx().coerceAtLeast(1f)
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                val strokeStyle = Stroke(width = stroke, cap = style.cap)
                val startAngle = style.startAngle
                val sweepAngle = style.sweepAngle
                val capAngle = asin((stroke / diameter).coerceIn(0f, 1f)) * 180f / PI.toFloat()

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
                        // Round caps stay inside the measured share instead of adding a visible minimum.
                        val direction = if (progressSweep > 0f) 1f else -1f
                        val capInset = if (style.cap == StrokeCap.Round && animatedProgress < 1f) capAngle else 0f
                        if (kotlin.math.abs(progressSweep) <= capInset * 2f) {
                            val midAngle = (startAngle + progressSweep / 2f) * PI.toFloat() / 180f
                            drawCircle(
                                color = animatedIndicatorColor,
                                radius = diameter / 2f * sin(kotlin.math.abs(progressSweep) * PI.toFloat() / 360f),
                                center = Offset(
                                    x = center.x + diameter / 2f * cos(midAngle),
                                    y = center.y + diameter / 2f * sin(midAngle),
                                ),
                            )
                            return@onDrawBehind
                        }
                        drawArc(
                            color = animatedIndicatorColor,
                            startAngle = startAngle + direction * capInset,
                            sweepAngle = progressSweep - direction * capInset * 2f,
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
