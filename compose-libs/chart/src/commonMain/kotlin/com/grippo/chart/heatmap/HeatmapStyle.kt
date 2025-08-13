package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Immutable
public data class HeatmapStyle(
    val layout: Layout,
    val labels: Labels,
    val legend: Legend,
    val palette: Palette,
    val cells: Cells,
    val values: Values,
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
        val gap: Dp,
        val corner: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public data class Labels(
        val showRowLabels: Boolean,
        val showColLabels: Boolean,
        val textStyle: TextStyle,
    )

    @Immutable
    public data class Legend(
        val show: Boolean,
        val height: Dp,
        val stops: List<Pair<Float, Color>>?,
        val labelStyle: TextStyle,
        val minText: ((Float) -> String)?,
        val maxText: ((Float) -> String)?,
    )

    @Immutable
    public data class Palette(
        val colorScale: (Float) -> Color,
        val autoNormalize: Boolean,
        val missingCellColor: Color?,
    )

    @Immutable
    public data class Cells(
        val showBorders: Boolean,
        val borderColor: Color,
        val borderWidth: Dp,
    )

    @Immutable
    public data class Values(
        val show: Boolean,
        val textStyle: TextStyle,
        val formatter: (Float, HeatmapData) -> String,
    )
}