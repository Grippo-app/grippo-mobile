package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
public data class SparklineStyle(
    val line: Line,
    val fill: Fill?,
    val baseline: Baseline,
    val dots: Dots,
    val extremes: Extremes,
) {
    @Immutable
    public data class Line(
        val stroke: Dp,
        val color: Color,
        val brush: ((Rect) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,    // 0..0.5
        val clampOvershoot: Boolean,
    )

    @Immutable
    public data class Fill(
        val provider: ((Rect) -> Brush)?,
    )

    @Immutable
    public sealed interface Baseline {
        @Immutable
        public data object None : Baseline

        @Immutable
        public data class Visible(
            val value: Float?,    // null → min value
            val color: Color,
            val width: Dp,
        ) : Baseline
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