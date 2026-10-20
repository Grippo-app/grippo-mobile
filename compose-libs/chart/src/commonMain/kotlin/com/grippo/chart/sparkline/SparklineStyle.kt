package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
public data class SparklineStyle(
    val line: Line,
    val fill: Fill?,
    val baseline: Baseline,
    val midline: Midline = Midline.None,
    val peek: Peek = Peek.None,
    val dots: Dots,
    val extremes: Extremes,
    val verticalPadding: Dp = 0.dp,
) {
    @Stable
    public data class Line(
        val stroke: Dp,
        val color: Color,
        val brush: ((Rect) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,
        val clampOvershoot: Boolean,
    )

    @Stable
    public data class Fill(
        val provider: ((Rect) -> Brush)?,
    )

    @Immutable
    public sealed interface Baseline {
        @Immutable
        public data object None : Baseline

        @Immutable
        public data class Visible(
            val value: Float?,
            val color: Color,
            val width: Dp,
        ) : Baseline
    }

    @Immutable
    public sealed interface Midline {
        @Immutable
        public data object None : Midline

        @Immutable
        public data class Visible(
            val position: Float = 0.5f,
            val color: Color,
            val width: Dp,
            val dash: Dp,
            val gap: Dp,
        ) : Midline
    }

    @Stable
    public sealed interface Peek {
        @Immutable
        public data object None : Peek

        @Stable
        public data class Visible(
            val hitSlop: Dp,

            val guideColor: Color,
            val guideWidth: Dp,
            val guideDash: Dp,
            val guideGap: Dp,

            val focusColor: Color? = null,
            val focusRadius: Dp,
            val focusRingWidth: Dp,
            val focusHaloRadius: Dp,

            val tooltipBackground: Color,
            val tooltipBorder: Color,
            val tooltipText: Color,
            val tooltipCornerRadius: Dp,
            val tooltipPaddingH: Dp,
            val tooltipPaddingV: Dp,
            val tooltipMargin: Dp,

            val decimals: Int = 0,
            val showLabel: Boolean = true,
            val valueFormatter: ((Float) -> String)? = null,
            val tooltipBorderWidth: Dp = 1.dp,
            val tooltipLabelSpacing: Dp = 2.dp,
            val valueTextStyle: TextStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center),
            val labelTextStyle: TextStyle = TextStyle(fontSize = 10.sp, textAlign = TextAlign.Center),
        ) : Peek
    }

    @Immutable
    public sealed interface Dots {
        @Immutable
        public data object None : Dots

        @Immutable
        public data class Visible(
            val radius: Dp,
            val color: Color?,
        ) : Dots
    }

    @Immutable
    public sealed interface Extremes {
        @Immutable
        public data object None : Extremes

        @Immutable
        public data class Visible(
            val minColor: Color,
            val maxColor: Color,
            val radius: Dp,
        ) : Extremes
    }
}
