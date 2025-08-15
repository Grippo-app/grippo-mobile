package com.grippo.chart.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a Sparkline (tiny line chart).
 *
 * Layout overview:
 * - Line: stroke, optional gradient brush, curvature and overshoot clamping
 * - Fill: optional area fill under the line
 * - Baseline: optional horizontal baseline
 * - Dots: small markers on points
 * - Extremes: highlight min/max points
 */
@Immutable
public data class SparklineStyle(
    val line: Line,
    val fill: Fill?,
    val baseline: Baseline,
    val dots: Dots,
    val extremes: Extremes,
) {
    /** Line stroke config and smoothing. */
    @Immutable
    public data class Line(
        val stroke: Dp,
        val color: Color,
        val brush: ((Rect) -> Brush)?,
        val curved: Boolean,
        val curveSmoothness: Float,    // 0..0.5
        val clampOvershoot: Boolean,
    )

    /** Optional fill under line. */
    @Immutable
    public data class Fill(
        val provider: ((Rect) -> Brush)?,
    )

    @Immutable
    public sealed interface Baseline {
        /** No baseline. */
        @Immutable
        public data object None : Baseline

        /** Visible baseline at [value] (null → min). */
        @Immutable
        public data class Visible(
            val value: Float?,    // null → min value
            val color: Color,
            val width: Dp,
        ) : Baseline
    }

    @Immutable
    public sealed interface Dots {
        /** No dots. */
        @Immutable
        public data object None : Dots

        /** Draw dots on points. */
        @Immutable
        public data class Visible(
            val radius: Dp,
            val color: Color?,
        ) : Dots
    }

    @Immutable
    public sealed interface Extremes {
        /** No extremes. */
        @Immutable
        public data object None : Extremes

        /** Highlight min/max points. */
        @Immutable
        public data class Visible(
            val minColor: Color,
            val maxColor: Color,
            val radius: Dp,
        ) : Extremes
    }
}