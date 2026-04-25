package com.grippo.design.components.indicators.internal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

private const val PROGRESS_ANIMATION_DURATION_MS = 500
private const val PROGRESS_ANIMATION_LABEL = "LineIndicatorBarProgress"

@Composable
internal fun LineIndicatorBar(
    modifier: Modifier,
    progress: Float,
    style: LineIndicatorStyle,
    barHeight: Dp,
    marker: (@Composable () -> Unit)?,
) {
    val sanitizedProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = sanitizedProgress,
        animationSpec = tween(
            durationMillis = PROGRESS_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = PROGRESS_ANIMATION_LABEL,
    )

    Layout(
        modifier = modifier
            .defaultMinSize(minHeight = barHeight)
            .drawWithCache {
                // Everything that depends only on size + style is computed once
                // and reused across animation frames. Cache invalidates when the
                // canvas size or any captured input (style, barHeight) changes.
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barPx = barHeight.toPx().coerceAtMost(canvasHeight)
                val radius = barPx / 2f
                val barTop = (canvasHeight - barPx) / 2f
                val barTopLeft = Offset(0f, barTop)
                val trackSize = Size(canvasWidth, barPx)
                val corner = CornerRadius(radius, radius)
                val fillBrush: Brush = when (style) {
                    is LineIndicatorStyle.Solid -> SolidColor(style.color)
                    is LineIndicatorStyle.Gradient -> {
                        val stops = style.stops
                        if (stops == null) {
                            Brush.horizontalGradient(
                                colors = style.colors,
                                startX = 0f,
                                endX = canvasWidth,
                            )
                        } else {
                            Brush.horizontalGradient(
                                colorStops = stops.zip(style.colors).toTypedArray(),
                                startX = 0f,
                                endX = canvasWidth,
                            )
                        }
                    }
                }

                onDrawBehind {
                    if (canvasWidth <= 0f || canvasHeight <= 0f) return@onDrawBehind

                    drawRoundRect(
                        color = style.track,
                        topLeft = barTopLeft,
                        size = trackSize,
                        cornerRadius = corner,
                    )

                    val progressWidth = canvasWidth * animatedProgress
                    if (progressWidth > 0f) {
                        drawRoundRect(
                            brush = fillBrush,
                            topLeft = barTopLeft,
                            size = Size(progressWidth, barPx),
                            cornerRadius = corner,
                        )
                    }
                }
            },
        content = {
            if (marker != null) {
                Box(contentAlignment = Alignment.Center) { marker() }
            }
        },
    ) { measurables, constraints ->
        val markerPlaceable = measurables.firstOrNull()?.measure(Constraints())
        val markerH = markerPlaceable?.height ?: 0
        val barPx = barHeight.roundToPx()
        val desiredH = maxOf(barPx, markerH)

        val width = when {
            constraints.hasBoundedWidth -> constraints.maxWidth
            else -> constraints.minWidth
        }
        val height = desiredH.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            if (markerPlaceable != null) {
                val markerW = markerPlaceable.width
                val halfW = markerW / 2
                val progressX = (width * animatedProgress).toInt()
                val maxCenter = (width - halfW).coerceAtLeast(halfW)
                val centerX = progressX.coerceIn(halfW, maxCenter)
                val centerY = height / 2
                markerPlaceable.place(
                    x = centerX - halfW,
                    y = centerY - markerH / 2,
                )
            }
        }
    }
}
