package com.grippo.chart.area

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual configuration for an Area chart (line + filled area under the line).
 *
 * Layout overview:
 * - Grid: horizontal guide lines inside the chart rect
 * - Y axis: labels/ticks at the left, optional axis line
 * - X axis: labels along the bottom (show all or adaptively thin)
 * - Line: polyline (curved/linear) over the data points
 * - Fill: optional gradient under the line (clipped to bottom of chart)
 * - Dots: small circles on each point
 * - Extrema: highlight min/max points with markers and labels
 */
@Immutable
public data class AreaStyle(
    val grid: Grid,
    val yAxis: YAxis,
    val yAxisLine: AxisLine?,
    val xAxis: XAxis,
    val line: Line,
    val glow: Glow,
    val fill: Fill?,
    val dots: Dots,
    val extrema: Extrema,
    val labelPadding: Dp = 6.dp,
) {
    /** Horizontal grid inside chart area. */
    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp
    )

    @Immutable
    public sealed interface YAxis {
        /** Hide Y axis. */
        @Immutable
        public data object None : YAxis

        /** Y axis labels with tick marks. */
        @Immutable
        public data class Labels(
            val targetTicks: Int,
            val textStyle: TextStyle,
            val formatter: (Float, AreaData) -> String,
            val tickMarkColor: Color,
            val tickMarkWidth: Dp,
        ) : YAxis
    }

    @Immutable
    public sealed interface XAxis {
        /** Hide X axis. */
        @Immutable
        public data object None : XAxis

        /** Draw all labels (may overlap). */
        @Immutable
        public data class LabelsShowAll(
            val textStyle: TextStyle,
        ) : XAxis

        /** Thin labels so they don't overlap; keep spacing >= [minGapDp]. */
        @Immutable
        public data class LabelsAdaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp,
        ) : XAxis
    }

    /** Line stroke configuration (color/gradient, curvature). */
    @Immutable
    public data class Line(
        val strokeWidth: Dp,
        val color: Color,
        val brushProvider: ((Size) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,
        val clampOvershoot: Boolean
    )

    /** Optional glow around the line (broad translucent stroke). */
    @Immutable
    public data class Glow(
        val width: Dp,
        val color: Color?
    )

    /** Area fill under the line. */
    @Immutable
    public data class Fill(
        val brushProvider: (Size) -> Brush
    )

    @Immutable
    public sealed interface Dots {
        /** No dots. */
        @Immutable
        public data object None : Dots

        /** Draw small dots at each data point. */
        @Immutable
        public data class Visible(
            val radius: Dp,
            val color: Color?
        ) : Dots
    }

    @Immutable
    public sealed interface Extrema {
        /** No extrema markers. */
        @Immutable
        public data object None : Extrema

        /** Mark min/max with circular markers and optional labels. */
        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            val markerRadius: Dp,
            val markerColor: Color?
        ) : Extrema
    }

    /** Vertical Y axis line at the left side. */
    @Immutable
    public data class AxisLine(
        val color: Color,
        val width: Dp,
    )
}