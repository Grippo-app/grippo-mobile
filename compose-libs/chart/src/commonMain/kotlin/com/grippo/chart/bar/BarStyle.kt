package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual configuration for a vertical Bar chart.
 *
 * Layout overview:
 * - Grid: horizontal lines across the chart
 * - Y axis: labels/ticks; optional axis line
 * - X axis: labels show-all or adaptive thinning
 * - Bars: per-entry fill (solid/gradient), stroke and corner radius
 * - Values: labels above/inside/outside bars
 * - Baseline/Target: optional reference lines
 */
@Immutable
public data class BarStyle(
    val layout: Layout,
    val grid: Grid,
    val yAxis: YAxis,
    val yAxisLine: AxisLine?,
    val xAxis: XAxis,
    val xBaseline: Baseline?,
    val bars: Bars,
    val values: Values,
) {
    /** Global paddings for labels. */
    @Immutable
    public data class Layout(
        val labelPadding: Dp,
    )

    /** Grid config (horizontal lines). */
    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp,
    )

    @Immutable
    public sealed interface YAxis {
        /** Hide Y axis. */
        @Immutable
        public data object None : YAxis

        /** Y axis labels with tick marks. */
        @Immutable
        public data class Labels(
            val ticks: Int,
            val textStyle: TextStyle,
            val formatter: (Float, BarData) -> String,
            val tickMarkColor: Color,
            val tickMarkWidth: Dp,
        ) : YAxis
    }

    @Immutable
    public sealed interface XAxis {
        /** Hide X axis. */
        @Immutable
        public data object None : XAxis

        /** Show all X labels (may overlap). */
        @Immutable
        public data class LabelsShowAll(
            val textStyle: TextStyle,
        ) : XAxis

        /** Adaptively thin labels to ensure a minimum gap. */
        @Immutable
        public data class LabelsAdaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp = 4.dp,
        ) : XAxis
    }

    /** Visual config for bars (corner, fill brush, stroke, sizing). */
    @Immutable
    public data class Bars(
        val corner: Dp,
        val brushProvider: ((BarEntry, Size, Rect) -> Brush)?,
        val strokeWidth: Dp,
        val strokeColor: Color,
        val sizing: BarsSizing,
    )

    @Immutable
    public sealed interface Values {
        /** No value labels. */
        @Immutable
        public data object None : Values

        /** Draw value labels above bars. */
        @Immutable
        public data class Above(
            val textStyle: TextStyle,
            val formatter: (Float, BarData) -> String,
        ) : Values
    }

    @Immutable
    public sealed interface BarsSizing {
        /** Compute equal bar/gap width to fill the chart (now adaptive under high density). */
        @Immutable
        public data class AutoEqualBarsAndGaps(
            val midThresholdDp: Dp = 16.dp,    // if wEqual < this -> use midRatio
            val denseThresholdDp: Dp = 10.dp,  // if wEqual < this -> use denseRatio
            val midRatio: Float = 0.5f,       // gap = midRatio * bar
            val denseRatio: Float = 0.3f      // gap = denseRatio * bar
        ) : BarsSizing

        /** Fixed bar width; gaps auto-fit to available width. */
        @Immutable
        public data class FixedBarWidth(val width: Dp) : BarsSizing

        /** Explicit bar width and spacing. */
        @Immutable
        public data class Explicit(val width: Dp, val spacing: Dp) : BarsSizing

        /** Gap = ratio * barWidth; barWidth is solved from chart width. */
        @Immutable
        public data class GapAsBarRatio(val ratio: Float) : BarsSizing
    }

    /** Left vertical axis line. */
    @Immutable
    public data class AxisLine(
        val color: Color,
        val width: Dp,
    )

    /** X=0 baseline line. */
    @Immutable
    public data class Baseline(
        val color: Color,
        val width: Dp,
    )
}