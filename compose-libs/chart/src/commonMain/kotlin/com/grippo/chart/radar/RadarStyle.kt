package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual configuration for a Radar (spider) chart.
 *
 * Axes radiate from the center and series are drawn as polygons. Use this style to control
 * rings, spokes, labels, polygon appearance, vertex markers, value labels and color mapping.
 *
 * Visual map:
 *   center
 *     •
 *     |\\        Spokes (radial lines)
 *     | \\      -> clockwise=true (angles increase clockwise)
 * ----+--o-----  Grid rings (circle or polygon)
 *     | /  o    Vertices (per-axis points)
 *     |/        Axis labels sit just outside polygon radius
 *
 * Color stops define a radial gradient from center (t=0f) to outer radius (t=1f).
 */
@Immutable
public data class RadarStyle(
    val layout: Layout,
    val grid: Grid,
    val spokes: Spokes,
    val labels: Labels,
    val polygon: Polygon,
    val vertices: Vertices,
    val values: Values,
    val dataPolicy: DataPolicy,
    val colorStops: List<Pair<Float, Color>>,
) {
    /** Overall layout knobs. */
    @Immutable
    public data class Layout(
        val labelPadding: Dp,
        val startAngleDeg: Float,
        val clockwise: Boolean,
    )

    /** Concentric grid rings. */
    @Immutable
    public data class Grid(
        val levels: Int,
        val asPolygon: Boolean,
        val color: Color,
        val strokeWidth: Dp,
        val levelLabels: LevelLabels,
    )

    @Immutable
    public sealed interface LevelLabels {
        /** Hide level labels. */
        @Immutable
        public data object None : LevelLabels

        /** Show labels for each ring (e.g., 0.2, 0.4, ...). */
        @Immutable
        public data class Visible(
            val levelLabelStyle: TextStyle,
            val levelFormatter: (Float) -> String,
        ) : LevelLabels
    }

    /** Radial lines from center along axes. */
    @Immutable
    public sealed interface Spokes {
        /** No spokes. */
        @Immutable
        public data object None : Spokes

        /** Visible spokes with color and width. */
        @Immutable
        public data class Visible(
            val color: Color,
            val strokeWidth: Dp,
        ) : Spokes
    }

    /** Axis labels at spoke ends. */
    @Immutable
    public sealed interface Labels {
        /** No axis labels. */
        @Immutable
        public data object None : Labels

        /** Visible axis labels. */
        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
        ) : Labels
    }

    /** Polygon appearance for series (stroke+fill). */
    @Immutable
    public data class Polygon(
        val strokeWidth: Dp,
        val strokeColorFallback: Color,
        val fillAlpha: Float,
    )

    /** Circular vertex markers. */
    @Immutable
    public sealed interface Vertices {
        /** No vertices. */
        @Immutable
        public data object None : Vertices

        /** Visible circular markers.
         * [colorOverride]=null -> auto color from gradient/value. */
        @Immutable
        public data class Visible(
            val radius: Dp,
            val colorOverride: Color?,
        ) : Vertices
    }

    /** Value labels near vertices. */
    @Immutable
    public sealed interface Values {
        /** No value labels. */
        @Immutable
        public data object None : Values

        /** Visible value labels. */
        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            val formatter: (Float, RadarData) -> String,
            val offset: Dp,
        ) : Values
    }

    /** Handling of partial series / null values. */
    @Immutable
    public data class DataPolicy(
        val requireCompleteSeries: Boolean,
        val missingAsZero: Boolean,
    )
}