package com.grippo.chart.radar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun radarData(): RadarData {
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
            color = Color(0xFF6AA9FF),
            values = RadarValues.ByAxisId(
                mapOf(
                    "chest" to 0.75f, "back" to 0.6f, "legs" to 0.9f,
                    "shoulders" to 0.55f, "arms" to 0.7f, "core" to 0.5f
                )
            )
        )
    )
    return RadarData(axes = axes, series = series)
}

private fun radarTwoAxes(): RadarData {
    val axes = listOf(RadarAxis("a", "A"), RadarAxis("b", "B"))
    val series = listOf(
        RadarSeries(name = "Two", color = Color(0xFFB049F8), values = RadarValues.ByIndex(listOf(0.3f, 0.9f)))
    )
    return RadarData(axes = axes, series = series)
}

private fun radarStyle(labels: Boolean, values: Boolean): RadarStyle = RadarStyle(
    layout = RadarStyle.Layout(labelPadding = 12.dp, startAngleDeg = -90f, clockwise = true),
    grid = RadarStyle.Grid(
        levels = 5, asPolygon = true, color = Color(0x14000000), strokeWidth = 1.dp,
        levelLabels = RadarStyle.LevelLabels.None
    ),
    spokes = RadarStyle.Spokes.Visible(color = Color(0x14000000), strokeWidth = 1.dp),
    labels = if (labels) RadarStyle.Labels.Visible(
        textStyle = TextStyle(color = Color(0xFF333333))
    ) else RadarStyle.Labels.None,
    polygon = RadarStyle.Polygon(
        strokeWidth = 2.dp,
        strokeColorFallback = Color(0xFF6AA9FF),
        fillAlpha = 0.35f
    ),
    vertices = RadarStyle.Vertices.Visible(radius = 3.dp, colorOverride = null),
    values = if (values) RadarStyle.Values.Visible(
        textStyle = TextStyle(color = Color(0xFF333333)),
        formatter = { v, _ -> (v * 100).toInt().toString() + "%" },
        offset = 10.dp
    ) else RadarStyle.Values.None,
    dataPolicy = RadarStyle.DataPolicy(requireCompleteSeries = true, missingAsZero = true)
)

@Preview
@Composable
private fun PreviewRadarLabelsOnly() {
    RadarChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = radarData(),
        style = radarStyle(labels = true, values = false)
    )
}

@Preview
@Composable
private fun PreviewRadarLabelsValues() {
    RadarChart(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        data = radarData(),
        style = radarStyle(labels = true, values = true)
    )
}

@Preview
@Composable
private fun PreviewRadarTwoAxes() {
    RadarChart(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        data = radarTwoAxes(),
        style = radarStyle(labels = true, values = true)
    )
}



