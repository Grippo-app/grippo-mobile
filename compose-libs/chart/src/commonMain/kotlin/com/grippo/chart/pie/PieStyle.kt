package com.grippo.chart.pie

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a donut-style Pie chart.
 *
 * Layout overview:
 * - Layout: outer padding and start angle
 * - Arc: ring thickness, corner radius and slice padding angle
 * - Labels: switch between inside/outside placement based on slice angle; formatter builds text
 * - Leaders: connector lines for outside labels
 */
@Immutable
public data class PieStyle(
    val layout: Layout,
    val arc: Arc,
    val labels: Labels,
    val leaders: Leaders,
) {
    /** Chart padding and first slice angle (deg). */
    @Immutable
    public data class Layout(
        val padding: Dp,
        val startAngleDeg: Float,
    )

    /** Slice ring configuration. */
    @Immutable
    public data class Arc(
        val width: Dp,
        val paddingAngleDeg: Float,
        val cornerRadius: Dp,
        /**
         * Minimal visual percent per slice (0..1). If > 0, small slices are clamped up to this
         * minimal fraction of the whole and the rest is renormalized to keep 360°.
         * This intentionally distorts visual angles (not data percentages).
         */
        val minVisualPercent: Float = 0f,
    )

    /** Labels inside/outside with threshold angles. */
    @Immutable
    public sealed class Labels {

        @Immutable
        public data class Adaptive(
            val insideMinAngleDeg: Float,
            val outsideMinAngleDeg: Float,
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()

        @Immutable
        public data class Inside(
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()

        @Immutable
        public data class Outside(
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()
    }

    /** Leader (connector) lines for outside labels. */
    @Immutable
    public data class Leaders(
        val show: Boolean,
        val lineWidth: Dp,
        val offset: Dp,
    )
}