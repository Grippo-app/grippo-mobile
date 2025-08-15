package com.grippo.chart.area

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp
    )

    @Immutable
    public sealed interface YAxis {
        @Immutable
        public data object None : YAxis

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
        @Immutable
        public data object None : XAxis

        @Immutable
        public data class LabelsShowAll(
            val textStyle: TextStyle,
        ) : XAxis

        @Immutable
        public data class LabelsAdaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp,
        ) : XAxis
    }

    @Immutable
    public data class Line(
        val strokeWidth: Dp,
        val color: Color,
        val brushProvider: ((Size) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,
        val clampOvershoot: Boolean
    )

    @Immutable
    public data class Glow(
        val width: Dp,
        val color: Color?
    )

    @Immutable
    public data class Fill(
        val brushProvider: (Size) -> Brush
    )

    @Immutable
    public sealed interface Dots {
        @Immutable
        public data object None : Dots

        @Immutable
        public data class Visible(
            val radius: Dp,
            val color: Color?
        ) : Dots
    }

    @Immutable
    public sealed interface Extrema {
        @Immutable
        public data object None : Extrema

        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            val markerRadius: Dp,
            val markerColor: Color?
        ) : Extrema
    }

    @Immutable
    public data class AxisLine(
        val color: Color,
        val width: Dp,
    )
}