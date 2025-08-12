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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarChart
import com.grippo.chart.radar.RadarStyle
import com.grippo.design.preview.AppPreview
import kotlin.math.roundToInt

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier
) {
    val axes = listOf(
        RadarAxis("Chest", 0.75f),
        RadarAxis("Back", 0.6f),
        RadarAxis("Legs", 0.9f),
        RadarAxis("Shoulders", 0.55f),
        RadarAxis("Arms", 0.7f),
        RadarAxis("Core", 0.5f),
    )

    val style = RadarStyle(
        padding = 12.dp,
        labelPadding = 12.dp,
        levels = 5,
        gridAsPolygon = true,
        gridColor = Color(0x33FFFFFF),
        gridStrokeWidth = 1.dp,
        showSpokes = true,
        spokeColor = Color(0x22FFFFFF),
        spokeStrokeWidth = 1.dp,
        strokeWidth = 2.dp,
        strokeColor = Color(0xFFB049F8),
        fillBrush = Brush.radialGradient(
            listOf(
                Color(0xFFB049F8).copy(alpha = 0.35f),
                Color.Transparent
            )
        ),
        showVertexDots = true,
        vertexDotRadius = 3.dp,
        vertexDotColor = null,
        showAxisLabels = true,
        labelStyle = TextStyle(color = Color(0x77FFFFFF)),
        showLevelLabels = false,
        levelLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
        levelFormatter = { v -> "${(v * 100f).roundToInt()}%" },
        showVertexValues = false,
        vertexValueStyle = TextStyle(color = Color(0xCCFFFFFF)),
        vertexValueFormatter = { v -> "${(v * 100f).roundToInt()}%" },
        startAngleDeg = -90f,
        clockwise = true,
    )

    RadarChart(
        modifier = modifier,
        axes = axes,
        style = style
    )
}

@AppPreview
@Composable
private fun RadarChartPreview() {
    val axes = listOf(
        RadarAxis("Chest", 0.75f),
        RadarAxis("Back", 0.6f),
        RadarAxis("Legs", 0.9f),
        RadarAxis("Shoulders", 0.55f),
        RadarAxis("Arms", 0.7f),
        RadarAxis("Core", 0.5f),
    )

    val style = RadarStyle(
        padding = 12.dp,
        labelPadding = 12.dp,
        levels = 5,
        gridAsPolygon = true,
        gridColor = Color(0x33FFFFFF),
        gridStrokeWidth = 1.dp,
        showSpokes = true,
        spokeColor = Color(0x22FFFFFF),
        spokeStrokeWidth = 1.dp,
        strokeWidth = 2.dp,
        strokeColor = Color(0xFFB049F8),
        fillBrush = Brush.radialGradient(
            listOf(
                Color(0xFFB049F8).copy(alpha = 0.35f),
                Color.Transparent
            )
        ),
        showVertexDots = true,
        vertexDotRadius = 3.dp,
        vertexDotColor = null,
        showAxisLabels = true,
        labelStyle = TextStyle(color = Color(0x77FFFFFF)),
        showLevelLabels = false,
        levelLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
        levelFormatter = { v -> "${(v * 100f).roundToInt()}%" },
        showVertexValues = false,
        vertexValueStyle = TextStyle(color = Color(0xCCFFFFFF)),
        vertexValueFormatter = { v -> "${(v * 100f).roundToInt()}%" },
        startAngleDeg = -90f,
        clockwise = true,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        RadarChart(
            modifier = Modifier.fillMaxSize(),
            axes = axes,
            style = style
        )
    }
}
