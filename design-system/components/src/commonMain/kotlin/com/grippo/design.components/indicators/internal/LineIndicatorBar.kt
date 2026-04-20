package com.grippo.design.components.indicators.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
internal fun LineIndicatorBar(
    modifier: Modifier,
    progress: Float,
    style: LineIndicatorStyle,
    barHeight: Dp,
    marker: (@Composable () -> Unit)?,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)

    Layout(
        modifier = modifier
            .defaultMinSize(minHeight = barHeight)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                if (canvasWidth <= 0f || canvasHeight <= 0f) return@drawBehind

                val barPx = barHeight.toPx().coerceAtMost(canvasHeight)
                val radius = barPx / 2f
                val barTop = (canvasHeight - barPx) / 2f

                drawRoundRect(
                    color = style.track,
                    topLeft = Offset(0f, barTop),
                    size = Size(canvasWidth, barPx),
                    cornerRadius = CornerRadius(radius, radius),
                )

                val progressWidth = canvasWidth * coercedProgress
                if (progressWidth > 0f) {
                    val topLeft = Offset(0f, barTop)
                    val fillSize = Size(progressWidth, barPx)
                    val corner = CornerRadius(radius, radius)
                    when (style) {
                        is LineIndicatorStyle.Solid -> drawRoundRect(
                            color = style.color,
                            topLeft = topLeft,
                            size = fillSize,
                            cornerRadius = corner,
                        )

                        is LineIndicatorStyle.Gradient -> {
                            val stops = style.stops
                            val brush: Brush = if (stops == null) {
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
                            drawRoundRect(
                                brush = brush,
                                topLeft = topLeft,
                                size = fillSize,
                                cornerRadius = corner,
                            )
                        }
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
                val progressX = (width * coercedProgress).toInt()
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
