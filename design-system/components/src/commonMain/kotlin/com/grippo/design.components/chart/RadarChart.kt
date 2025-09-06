package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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

@Immutable
public data class DSRadarAxis(
    val id: String,
    val label: String
)

@Immutable
public data class DSRadarSeries(
    val name: String,
    val color: Color,
    val valuesByAxisId: Map<String, Float>, // 0..1
)

@Immutable
public data class DSRadarData(
    val axes: List<DSRadarAxis>,
    val series: List<DSRadarSeries>,
    val valueUnit: String? = null,
)

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier,
    data: DSRadarData
) {
    val charts = AppTokens.colors.charts
    val palette = AppTokens.colors.palette

    // --- convert colors -> (fraction, color) stops ---
    val scaleStopsOrangeRed: List<Pair<Float, Color>> = remember(palette.palette5OrangeRed) {
        val colors = palette.palette5OrangeRed
        val last = (colors.size - 1).coerceAtLeast(1)
        colors.mapIndexed { index, color ->
            index.toFloat() / last.toFloat() to color
        }
    }

    val style = RadarStyle(
        layout = RadarStyle.Layout(
            labelPadding = 12.dp,
            startAngleDeg = -90f,
            clockwise = true,
        ),
        grid = RadarStyle.Grid(
            levels = 5,
            asPolygon = true,
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp,
            levelLabels = RadarStyle.LevelLabels.None,
        ),
        spokes = RadarStyle.Spokes.Visible(
            color = AppTokens.colors.divider.default,
            strokeWidth = 1.dp,
        ),
        labels = RadarStyle.Labels.Visible(
            textStyle = AppTokens.typography.b10Reg().copy(color = AppTokens.colors.text.primary)
        ),
        polygon = RadarStyle.Polygon(
            strokeWidth = 2.dp,
            strokeColorFallback = charts.radar.strokeFallback,
            fillAlpha = 0.35f,
        ),
        vertices = RadarStyle.Vertices.Visible(
            radius = 3.dp,
            colorOverride = null,
        ),
        values = RadarStyle.Values.None,
        dataPolicy = RadarStyle.DataPolicy(
            requireCompleteSeries = true,
            missingAsZero = true,
        ),
        colorStops = scaleStopsOrangeRed, // now real stops
    )

    RadarChart(
        modifier = modifier,
        data = remember(data) { data.toChart() },
        style = style
    )
}

private fun DSRadarAxis.toChart(): RadarAxis = RadarAxis(
    id = id,
    label = label
)

private fun DSRadarSeries.toChart(): RadarSeries = RadarSeries(
    name = name,
    color = color,
    values = RadarValues.ByAxisId(valuesByAxisId)
)

private fun DSRadarData.toChart(): RadarData = RadarData(
    axes = axes.map { it.toChart() },
    series = series.map { it.toChart() },
    valueUnit = valueUnit,
)

@AppPreview
@Composable
private fun RadarChartPreview() {
    PreviewContainer {
        val ds = DSRadarData(
            axes = listOf(
                DSRadarAxis("chest", "Chest"),
                DSRadarAxis("back", "Back"),
                DSRadarAxis("legs", "Legs"),
                DSRadarAxis("shoulders", "Shoulders"),
                DSRadarAxis("arms", "Arms"),
                DSRadarAxis("core", "Core"),
            ),
            series = listOf(
                DSRadarSeries(
                    name = "Current",
                    color = Color(0xFFB049F8),
                    valuesByAxisId = mapOf(
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
            data = ds
        )
    }
}
