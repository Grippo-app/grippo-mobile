package com.grippo.design.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarChart
import com.grippo.chart.radar.RadarData
import com.grippo.chart.radar.RadarSeries
import com.grippo.chart.radar.RadarStyle
import com.grippo.chart.radar.RadarValues
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier
) {
    val axes = listOf(
        RadarAxis("chest", "Chest"),
        RadarAxis("back", "Back"),
        RadarAxis("legs", "Legs"),
        RadarAxis("shoulders", "Shoulders"),
        RadarAxis("arms", "Arms"),
        RadarAxis("core", "Core"),
    )

    val series = listOf(
        RadarSeries(
            name = "Current",
            color = Color(0xFFB049F8),
            values = RadarValues.ByAxisId(
                mapOf(
                    "chest" to 0.75f,
                    "back" to 0.6f,
                    "legs" to 0.9f,
                    "shoulders" to 0.55f,
                    "arms" to 0.7f,
                    "core" to 0.5f
                )
            )
        )
    )

    val data = RadarData(
        axes = axes,
        series = series,
        valueUnit = null,
    )

    val style = RadarStyle(
        layout = RadarStyle.Layout(
            padding = 12.dp,
            labelPadding = 12.dp,
            startAngleDeg = -90f,
            clockwise = true,
        ),
        grid = RadarStyle.Grid(
            levels = 5,
            asPolygon = true,
            color = Color(0x33FFFFFF),
            strokeWidth = 1.dp,
            showLevelLabels = false,
            levelLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
            levelFormatter = { v -> "${(v * 100f).roundToInt()}%" }
        ),
        spokes = RadarStyle.Spokes(
            show = true,
            color = Color(0x22FFFFFF),
            strokeWidth = 1.dp,
        ),
        labels = RadarStyle.Labels(
            show = true,
            textStyle = TextStyle(color = Color(0x77FFFFFF))
        ),
        polygon = RadarStyle.Polygon(
            strokeWidth = 2.dp,
            strokeColorFallback = Color(0xFFB049F8),
            fillAlpha = 0.35f,
        ),
        vertices = RadarStyle.Vertices(
            show = true,
            radius = 3.dp,
            colorOverride = null,
        ),
        values = RadarStyle.Values(
            show = false,
            textStyle = TextStyle(color = Color(0xCCFFFFFF)),
            formatter = { v, _ -> "${(v * 100f).roundToInt()}%" },
            offset = 8.dp,
        ),
        dataPolicy = RadarStyle.DataPolicy(
            requireCompleteSeries = true,
            missingAsZero = true,
        )
    )

    RadarChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun RadarChartPreview() {
    PreviewContainer {
        val axes = listOf(
            RadarAxis("chest", "Chest"),
            RadarAxis("back", "Back"),
            RadarAxis("legs", "Legs"),
            RadarAxis("shoulders", "Shoulders"),
            RadarAxis("arms", "Arms"),
            RadarAxis("core", "Core"),
        )
        val series = listOf(
            RadarSeries(
                name = "Current",
                color = Color(0xFFB049F8),
                values = RadarValues.ByAxisId(
                    mapOf(
                        "chest" to 0.75f,
                        "back" to 0.6f,
                        "legs" to 0.9f,
                        "shoulders" to 0.55f,
                        "arms" to 0.7f,
                        "core" to 0.5f
                    )
                )
            )
        )
        val data = RadarData(
            axes = axes,
            series = series,
            valueUnit = null,
        )
        val style = RadarStyle(
            layout = RadarStyle.Layout(
                padding = 12.dp,
                labelPadding = 12.dp,
                startAngleDeg = -90f,
                clockwise = true,
            ),
            grid = RadarStyle.Grid(
                levels = 5,
                asPolygon = true,
                color = Color(0x33FFFFFF),
                strokeWidth = 1.dp,
                showLevelLabels = false,
                levelLabelStyle = TextStyle(color = Color(0x66FFFFFF)),
                levelFormatter = { v -> "${(v * 100f).roundToInt()}%" }
            ),
            spokes = RadarStyle.Spokes(
                show = true,
                color = Color(0x22FFFFFF),
                strokeWidth = 1.dp,
            ),
            labels = RadarStyle.Labels(
                show = true,
                textStyle = TextStyle(color = Color(0x77FFFFFF))
            ),
            polygon = RadarStyle.Polygon(
                strokeWidth = 2.dp,
                strokeColorFallback = Color(0xFFB049F8),
                fillAlpha = 0.35f,
            ),
            vertices = RadarStyle.Vertices(
                show = true,
                radius = 3.dp,
                colorOverride = null,
            ),
            values = RadarStyle.Values(
                show = false,
                textStyle = TextStyle(color = Color(0xCCFFFFFF)),
                formatter = { v, _ -> "${(v * 100f).roundToInt()}%" },
                offset = 8.dp,
            ),
            dataPolicy = RadarStyle.DataPolicy(
                requireCompleteSeries = true,
                missingAsZero = true,
            )
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
                data = data,
                style = style
            )
        }
    }
}
