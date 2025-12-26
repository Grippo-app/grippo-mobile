package com.grippo.design.components.modifiers

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

@Immutable
public enum class SpotAlignment {
    TopStart,
    TopEnd,
    Center,
    BottomStart,
    BottomEnd;

    internal fun calculateOffset(size: Size): Offset {
        val x = when (this) {
            TopStart, BottomStart -> size.width * 0.15f
            TopEnd, BottomEnd -> size.width * 0.85f
            Center -> size.width / 2f
        }
        val y = when (this) {
            TopStart, TopEnd -> size.height * 0.15f
            BottomStart, BottomEnd -> size.height * 0.85f
            Center -> size.height / 2f
        }
        return Offset(x = x, y = y)
    }
}

public fun Modifier.spot(
    color: Color,
    alpha: Float = 0.4f,
    radiusMultiplier: Float = 1.25f,
    alignment: SpotAlignment = SpotAlignment.Center,
): Modifier = drawBehind {
    if (size.width == 0f || size.height == 0f) return@drawBehind

    val radius = max(size.width, size.height) * radiusMultiplier
    val center = alignment.calculateOffset(size)
    val brush = Brush.radialGradient(
        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
        center = center,
        radius = radius
    )

    drawCircle(
        brush = brush,
        radius = radius,
        center = center
    )
}
