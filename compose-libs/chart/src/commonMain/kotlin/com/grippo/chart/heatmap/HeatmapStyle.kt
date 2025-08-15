package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a Heatmap chart (matrix of colored cells).
 *
 * Layout overview:
 * - Layout: cell gap, corner radius, label padding
 * - Row/Column labels: show all or adaptively thin to avoid overlap
 * - Legend: gradient bar with min/max labels
 * - Palette: color scale function and normalization mode
 * - Borders: optional cell borders
 * - Values: draw value labels inside cells
 */
@Immutable
public data class HeatmapStyle(
    val layout: Layout,
    val rowLabels: AxisLabels,
    val colLabels: AxisLabels,
    val legend: Legend,
    val palette: Palette,
    val borders: Borders,
    val values: Values,
) {
    /** Cell geometry and label paddings. */
    @Immutable
    public data class Layout(
        val gap: Dp,
        val corner: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public sealed interface AxisLabels {
        /** Hide axis labels. */
        @Immutable
        public data object None : AxisLabels

        /** Show all labels (may overlap). */
        @Immutable
        public data class ShowAll(
            val textStyle: TextStyle,
        ) : AxisLabels

        /** Adaptively thin labels to enforce a minimum gap. */
        @Immutable
        public data class Adaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp,
        ) : AxisLabels
    }

    @Immutable
    public sealed interface Legend {
        /** Hide legend. */
        @Immutable
        public data object None : Legend

        /** Gradient legend with optional min/max labels. */
        @Immutable
        public data class Visible(
            val height: Dp,
            val stops: List<Pair<Float, Color>>?,
            val labelStyle: TextStyle,
            val minText: ((Float) -> String)?,
            val maxText: ((Float) -> String)?,
        ) : Legend
    }

    /** Color scale and normalization options. */
    @Immutable
    public data class Palette(
        val colorScale: (Float) -> Color,
        val autoNormalize: Boolean,
        val missingCellColor: Color?,
    )

    @Immutable
    public sealed interface Borders {
        /** No borders. */
        @Immutable
        public data object None : Borders

        /** Draw borders around each cell. */
        @Immutable
        public data class Visible(
            val borderColor: Color,
            val borderWidth: Dp,
        ) : Borders
    }

    @Immutable
    public sealed interface Values {
        /** Hide cell values. */
        @Immutable
        public data object None : Values

        /** Draw normalized values inside cells using [textStyle]. */
        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            val formatter: (Float, HeatmapData) -> String,
        ) : Values
    }
}