package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val target: Target?,
) {
    @Immutable
    public data class Layout(
        val labelPadding: Dp,
    )

    @Immutable
    public data class Grid(
        val show: Boolean,
        val color: Color,
        val strokeWidth: Dp,
    )

    @Immutable
    public sealed interface YAxis {
        @Immutable
        public data object None : YAxis

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
        @Immutable
        public data object None : XAxis

        @Immutable
        public data class LabelsShowAll(
            val textStyle: TextStyle,
        ) : XAxis

        @Immutable
        public data class LabelsAdaptive(
            val textStyle: TextStyle,
            val minGapDp: Dp = 4.dp,
        ) : XAxis
    }

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
        @Immutable
        public data object None : Values

        @Immutable
        public data class Above(
            val textStyle: TextStyle,
            val formatter: (Float, BarData) -> String,
        ) : Values

        @Immutable
        public data class Outside(
            val textStyle: TextStyle,
            val formatter: (Float, BarData) -> String,
        ) : Values

        @Immutable
        public data class Inside(
            val textStyle: TextStyle,
            val formatter: (Float, BarData) -> String,
            val minInnerPadding: Dp,
            val insideColor: Color?,
        ) : Values
    }

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color,
        val width: Dp,
    )

    @Immutable
    public sealed interface BarsSizing {
        @Immutable
        public data object AutoEqualBarsAndGaps : BarsSizing

        @Immutable
        public data class FixedBarWidth(val width: Dp) : BarsSizing

        @Immutable
        public data class Explicit(val width: Dp, val spacing: Dp) : BarsSizing
    }

    @Immutable
    public data class AxisLine(
        val color: Color,
        val width: Dp,
    )

    @Immutable
    public data class Baseline(
        val color: Color,
        val width: Dp,
    )
}