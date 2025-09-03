package com.grippo.chart.progress

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a horizontal Progress chart (multi-bars).
 *
 * Layout overview:
 * - Domain: normalized [0..1] or absolute max
 * - Bars: track (background), foreground gradient, stroke
 * - Labels: left-side labels per row
 * - Values: inside or outside numeric labels (normalized or absolute)
 * - Target: optional vertical reference line
 */
@Immutable
public data class ProgressStyle(
    val layout: Layout,
    val domain: Domain,
    val bars: Bars,
    val labels: Labels,
    val values: Values,
    val target: Target?,
    val progression: Progression,
) {
    /** Row/bar geometry and paddings. */
    @Immutable
    public data class Layout(
        val barHeight: Dp,
        val spacing: Dp,
        val corner: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public sealed interface Domain {
        /** Values are already in [0..1]. */
        @Immutable
        public data object Normalized : Domain

        /** Values are absolute; [maxValue] can override the auto max. */
        @Immutable
        public data class Absolute(
            val maxValue: Float?,      // optional manual max; if null/<=0 -> auto
        ) : Domain
    }

    /** Track and foreground bar styling. */
    @Immutable
    public data class Bars(
        val trackColor: Color?,    // null disables track
        val brushProvider: ((ProgressChartData, Size, Rect) -> Brush)?,
        val strokeWidth: Dp,
        val strokeColor: Color,
    )

    /** Left-side labels style. */
    @Immutable
    public data class Labels(
        val textStyle: TextStyle,
    )

    @Immutable
    public sealed interface Values {
        /** Hide values. */
        @Immutable
        public data object None : Values

        /** Inside value labels (auto contrast by default). */
        @Immutable
        public data class Inside(
            val textStyle: TextStyle,
            val formatter: (Float, ProgressData) -> String,
            val minInnerPadding: Dp,
            val insideColor: Color?,              // null -> auto contrast
            val preferNormalizedLabels: Boolean,  // when domain is Normalized
        ) : Values

        /** Outside value labels (to the right of the bar). */
        @Immutable
        public data class Outside(
            val textStyle: TextStyle,
            val formatter: (Float, ProgressData) -> String,
            val preferNormalizedLabels: Boolean,  // when domain is Normalized
        ) : Values
    }

    /** Vertical target marker line. */
    @Immutable
    public data class Target(
        val value: Float,
        val color: Color,
        val width: Dp,
    )

    /** Progression scaling for bar widths. */
    @Immutable
    public sealed interface Progression {
        /** Linear scaling: barWidth = (v/max) * chartW */
        @Immutable
        public data object Linear : Progression

        /** Power scaling: barWidth = (v/max)^alpha * chartW */
        @Immutable
        public data class Power(val alpha: Float) : Progression

        /** Logarithmic scaling: barWidth = ln(v+1)/ln(max+1) * chartW */
        @Immutable
        public data object Logarithmic : Progression
    }
}