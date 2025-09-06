package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a Heatmap chart.
 * Minimal sizing API:
 *  - If [layout.maxCellSize] is null -> cells stretch to fit width (still squares).
 *  - If [layout.maxCellSize] is non-null -> cell side = min(fitToWidth, maxCellSize).
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
    @Immutable
    public data class Layout(
        val gap: Dp,
        val corner: Dp,
        val labelPadding: Dp,
        /** Optional cap for the square side. If null, squares stretch to fit width. */
        val maxCellSize: Dp?,
    )

    @Immutable
    public sealed interface AxisLabels {
        @Immutable
        public data object None : AxisLabels

        @Immutable
        public data class ShowAll(val textStyle: TextStyle) : AxisLabels

        @Immutable
        public data class Adaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp,
        ) : AxisLabels
    }

    @Immutable
    public sealed interface Legend {
        @Immutable
        public data object None : Legend

        @Immutable
        public data class Visible(
            val height: Dp,
            val labelStyle: TextStyle,
            val minText: ((Float) -> String)? = null,
            val maxText: ((Float) -> String)? = null,
        ) : Legend
    }

    @Immutable
    public data class Palette(
        val colorScale: (Float) -> Color,
        val autoNormalize: Boolean,
        val missingCellColor: Color?,
    )

    @Immutable
    public sealed interface Borders {
        @Immutable
        public data object None : Borders

        @Immutable
        public data class Visible(
            val borderColor: Color,
            val borderWidth: Dp,
        ) : Borders
    }

    @Immutable
    public sealed interface Values {
        @Immutable
        public data object None : Values

        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            /** Receives normalized value [0..1] and the original data. */
            val formatter: (Float, HeatmapData) -> String = { t, _ ->
                "${(t * 100f).toInt()}%"
            },
        ) : Values
    }
}