package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.radar.RadarAxis
import com.grippo.chart.radar.RadarChart
import com.grippo.chart.radar.RadarData
import com.grippo.chart.radar.RadarSeries
import com.grippo.chart.radar.RadarStyle
import com.grippo.chart.radar.RadarValues
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.roundToInt

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier
) {
    val charts = AppTokens.colors.charts

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
            color = charts.radar.palette[0],
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
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp,
            showLevelLabels = false,
            levelLabelStyle = AppTokens.typography.b11Reg()
                .copy(color = AppTokens.colors.text.secondary.copy(alpha = 0.85f)),
            levelFormatter = { v -> "${(v * 100f).roundToInt()}%" }
        ),
        spokes = RadarStyle.Spokes(
            show = true,
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp,
        ),
        labels = RadarStyle.Labels(
            show = true,
            textStyle = AppTokens.typography.b11Reg().copy(color = AppTokens.colors.text.primary)
        ),
        polygon = RadarStyle.Polygon(
            strokeWidth = 2.dp,
            strokeColorFallback = charts.radar.strokeFallback,
            fillAlpha = 0.35f,
        ),
        vertices = RadarStyle.Vertices(
            show = true,
            radius = 3.dp,
            colorOverride = null,
        ),
        values = RadarStyle.Values(
            show = false,
            textStyle = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.primary),
            formatter = { v, _ -> "${(v.coerceIn(0f, 1f) * 100f).roundToInt()}%" },
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
        listOf(
            RadarAxis("chest", "Chest"),
            RadarAxis("back", "Back"),
            RadarAxis("legs", "Legs"),
            RadarAxis("shoulders", "Shoulders"),
            RadarAxis("arms", "Arms"),
            RadarAxis("core", "Core"),
        )
        listOf(
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

        RadarChart(
            modifier = Modifier.size(300.dp),
        )
    }
}
