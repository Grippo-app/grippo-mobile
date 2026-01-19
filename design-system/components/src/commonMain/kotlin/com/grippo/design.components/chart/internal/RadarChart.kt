package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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

@Composable
internal fun RadarChart(
    modifier: Modifier = Modifier,
    data: RadarData,
    showLabels: Boolean
) {
    val charts = AppTokens.colors.charts
    val palette = AppTokens.colors.palette

    // --- convert colors -> (fraction, color) stops ---
    val scaleStopsOrangeRed: List<Pair<Float, Color>> = remember(palette.palette5OrangeRedGrowth) {
        val colors = palette.palette5OrangeRedGrowth
        val last = (colors.size - 1).coerceAtLeast(1)
        colors.mapIndexed { index, color ->
            index.toFloat() / last.toFloat() to color
        }
    }

    val labels = when (showLabels) {
        true -> RadarStyle.Labels.Visible(
            textStyle = AppTokens.typography.b10Reg().copy(color = AppTokens.colors.text.primary)
        )

        false -> RadarStyle.Labels.None
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
            color = AppTokens.colors.charts.radar.grid,
            strokeWidth = 1.dp,
            levelLabels = RadarStyle.LevelLabels.None,
        ),
        spokes = RadarStyle.Spokes.Visible(
            color = AppTokens.colors.charts.radar.grid,
            strokeWidth = 1.dp,
        ),
        peek = RadarStyle.Peek.Visible(
            hitSlop = 26.dp,

            guideColor = charts.tooltip.guide,
            guideWidth = 1.dp,
            guideDash = 6.dp,
            guideGap = 6.dp,

            focusColor = charts.tooltip.focus,
            focusRadius = 3.5.dp,
            focusRingWidth = 2.dp,
            focusHaloRadius = 18.dp,

            tooltipBackground = charts.tooltip.background,
            tooltipBorder = charts.tooltip.border,
            tooltipText = charts.tooltip.text,
            tooltipCornerRadius = 6.dp,
            tooltipPaddingH = 8.dp,
            tooltipPaddingV = 4.dp,
            tooltipMargin = 6.dp,

            decimals = 0,
            showAxisLabel = true,
            showSeriesName = false,
        ),
        labels = labels,
        polygon = RadarStyle.Polygon(
            strokeWidth = 1.5.dp,
            fillAlpha = 0.35f,
        ),
        vertices = RadarStyle.Vertices.Visible(
            radius = 2.dp,
            colorOverride = null,
        ),
        values = RadarStyle.Values.None,
        dataPolicy = RadarStyle.DataPolicy(
            requireCompleteSeries = true,
            missingAsZero = true,
        ),
        colorStops = scaleStopsOrangeRed,
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
        val ds = RadarData(
            axes = listOf(
                RadarAxis("chest", "Chest"),
                RadarAxis("back", "Back"),
                RadarAxis("legs", "Legs"),
                RadarAxis("shoulders", "Shoulders"),
                RadarAxis("arms", "Arms"),
                RadarAxis("core", "Core"),
            ),
            series = listOf(
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
        )

        RadarChart(
            modifier = Modifier.size(300.dp),
            data = ds,
            showLabels = true
        )
    }
}
