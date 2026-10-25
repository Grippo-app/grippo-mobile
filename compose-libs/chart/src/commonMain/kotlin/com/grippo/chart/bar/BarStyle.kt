package com.grippo.chart.bar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val peek: Peek = Peek.None,
) {
    @Immutable
    public data class Layout(
        val labelPadding: Dp,
        val chartPadding: ChartPadding = ChartPadding(),
        val minBarHeight: Dp = 0.dp,
        val yAxisLabelSpacing: Dp = labelPadding,
        val valueLabelSpacing: Dp = labelPadding,
        val baselineSpacing: Dp = 0.dp,
        val barsAxisInsetStart: Dp = 0.dp,
        val barsAxisInsetEnd: Dp = 0.dp,
        val xAxisLabelSpacing: Dp = labelPadding,
    )

    @Immutable
    public data class ChartPadding(
        val start: Dp = 0.dp,
        val top: Dp = 0.dp,
        val end: Dp = 0.dp,
        val bottom: Dp = 0.dp,
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
            val tickMarkLength: Dp = 4.dp,
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
    }

    @Immutable
    public sealed interface BarsSizing {
        @Immutable
        public data class EvenSlots(
            val maxBarWidth: Dp,
            val widthFraction: Float,
        ) : BarsSizing

        @Immutable
        public data class AutoEqualBarsAndGaps(
            val midThresholdDp: Dp = 16.dp,
            val denseThresholdDp: Dp = 10.dp,
            val midRatio: Float = 0.5f,
            val denseRatio: Float = 0.3f,
            val maxBarWidth: Dp = 40.dp
        ) : BarsSizing

        @Immutable
        public data class FixedBarWidth(val width: Dp) : BarsSizing

        @Immutable
        public data class Explicit(val width: Dp, val spacing: Dp) : BarsSizing

        @Immutable
        public data class GapAsBarRatio(val ratio: Float) : BarsSizing
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

    @Immutable
    public sealed interface Peek {
        @Immutable
        public data object None : Peek

        @Immutable
        public data class Visible(
            val hitSlop: Dp = 8.dp,
            val guideColor: Color,
            val guideWidth: Dp = 1.dp,
            val guideDash: Dp = 4.dp,
            val guideGap: Dp = 4.dp,
            val focusColor: Color? = null,
            val focusRadius: Dp = 4.dp,
            val focusRingWidth: Dp = 2.dp,
            val focusHaloRadius: Dp = 18.dp,
            val selectedBarOverlay: Color? = null,
            val selectedBarStrokeColor: Color? = null,
            val selectedBarStrokeWidth: Dp = 2.dp,
            val tooltipBackground: Color,
            val tooltipBorder: Color,
            val tooltipText: Color,
            val tooltipCornerRadius: Dp,
            val tooltipPaddingH: Dp,
            val tooltipPaddingV: Dp,
            val tooltipMargin: Dp,
            val decimals: Int = 0,
            val showLabel: Boolean = true,
            val valueFormatter: ((Float, BarData) -> String)? = null,
            val tooltipBorderWidth: Dp = 1.dp,
            val tooltipLabelSpacing: Dp = 2.dp,
            val valueTextStyle: TextStyle = TextStyle(
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            val labelTextStyle: TextStyle = TextStyle(
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            ),
        ) : Peek
    }
}
