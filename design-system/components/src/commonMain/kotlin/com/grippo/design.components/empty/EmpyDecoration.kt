package com.grippo.design.components.empty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun EmptyDecorations(modifier: Modifier = Modifier) {
    val brand = AppTokens.colors.brand
    val gridColor = AppTokens.colors.border.default.copy(alpha = 0.12f)
    val highlight = brand.color1.copy(alpha = 0.15f)
    val accent = brand.color4.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minDimension = size.minDimension.coerceAtLeast(1f)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(highlight, Color.Transparent)
            ),
            size = size
        )

        val verticalLines = 4
        val horizontalLines = 3
        val gridStroke = minDimension * 0.003f

        repeat(verticalLines + 1) { index ->
            val fraction = index / verticalLines.coerceAtLeast(1).toFloat()
            val x = width * (0.15f + fraction * 0.7f)
            drawLine(
                color = gridColor,
                start = Offset(x = x, y = height * 0.2f),
                end = Offset(x = x, y = height * 0.85f),
                strokeWidth = gridStroke
            )
        }

        repeat(horizontalLines + 1) { index ->
            val fraction = index / horizontalLines.coerceAtLeast(1).toFloat()
            val y = height * (0.25f + fraction * 0.6f)
            drawLine(
                color = gridColor,
                start = Offset(x = width * 0.1f, y = y),
                end = Offset(x = width * 0.9f, y = y),
                strokeWidth = gridStroke
            )
        }

        drawLine(
            color = accent,
            start = Offset(x = width * 0.1f, y = height * 0.85f),
            end = Offset(x = width * 0.9f, y = height * 0.3f),
            strokeWidth = gridStroke * 1.2f,
            cap = StrokeCap.Round
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brand.color3.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(width * 0.8f, height * 0.35f),
                radius = minDimension * 0.55f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brand.color2.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(width * 0.25f, height * 0.75f),
                radius = minDimension * 0.5f
            )
        )
    }
}

@AppPreview
@Composable
private fun EmptyDecorationsPreview() {
    PreviewContainer {
        EmptyDecorations(
            modifier = Modifier.fillMaxSize()
        )
    }
}
