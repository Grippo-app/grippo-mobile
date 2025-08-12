package com.grippo.design.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.sparkline.Sparkline
import com.grippo.chart.sparkline.SparklineStyle
import com.grippo.design.preview.AppPreview

@Composable
public fun Sparkline(
    modifier: Modifier = Modifier
) {
    val values = listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f)

    val style = SparklineStyle(
        stroke = 2.dp,
        color = Color(0xFF6AA9FF),
        lineBrush = { rect ->
            Brush.horizontalGradient(
                listOf(Color(0xFF6AA9FF), Color(0xFF00E6A7)),
                startX = rect.left,
                endX = rect.right
            )
        },
        curved = true,
        curveSmoothness = 0.25f,
        clampOvershoot = true,
        fill = { rect ->
            Brush.verticalGradient(
                0f to Color(0xFF6AA9FF).copy(alpha = 0.18f),
                1f to Color(0xFF6AA9FF).copy(alpha = 0f),
                startY = rect.top,
                endY = rect.bottom
            )
        },
        showBaseline = false,
        baselineValue = null,
        baselineColor = Color(0x33FFFFFF),
        baselineWidth = 1.dp,
        showDots = false,
        dotRadius = 2.dp,
        dotColor = null,
        showMinMax = true,
        minColor = Color(0xFFFF7A33),
        maxColor = Color(0xFF00E6A7),
        minMaxRadius = 3.dp,
        padding = 6.dp,
    )

    Sparkline(
        modifier = modifier,
        values = values,
        style = style
    )
}

@AppPreview
@Composable
private fun SparklinePreview() {
    val values = listOf(4f, 6f, 5f, 8f, 9f, 7f, 12f, 10f, 13f, 11f, 16f)

    val style = SparklineStyle(
        stroke = 2.dp,
        color = Color(0xFF6AA9FF),
        lineBrush = { rect ->
            Brush.horizontalGradient(
                listOf(Color(0xFF6AA9FF), Color(0xFF00E6A7)),
                startX = rect.left,
                endX = rect.right
            )
        },
        curved = true,
        curveSmoothness = 0.25f,
        clampOvershoot = true,
        fill = { rect ->
            Brush.verticalGradient(
                0f to Color(0xFF6AA9FF).copy(alpha = 0.18f),
                1f to Color(0xFF6AA9FF).copy(alpha = 0f),
                startY = rect.top,
                endY = rect.bottom
            )
        },
        showBaseline = false,
        baselineValue = null,
        baselineColor = Color(0x33FFFFFF),
        baselineWidth = 1.dp,
        showDots = false,
        dotRadius = 2.dp,
        dotColor = null,
        showMinMax = true,
        minColor = Color(0xFFFF7A33),
        maxColor = Color(0xFF00E6A7),
        minMaxRadius = 3.dp,
        padding = 6.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFF0F172A))
            .padding(12.dp)
    ) {
        Sparkline(
            modifier = Modifier.fillMaxSize(),
            values = values,
            style = style
        )
    }
}
