package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

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
    @Immutable
    public data class Layout(
        val labelPadding: Dp,
        val startAngleDeg: Float,
        val clockwise: Boolean,
    )

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
        @Immutable
        public data object None : LevelLabels

        @Immutable
        public data class Visible(
            val levelLabelStyle: TextStyle,
            val levelFormatter: (Float) -> String,
        ) : LevelLabels
    }

    @Immutable
    public sealed interface Spokes {
        @Immutable
        public data object None : Spokes

        @Immutable
        public data class Visible(
            val color: Color,
            val strokeWidth: Dp,
        ) : Spokes
    }

    @Immutable
    public sealed interface Labels {
        @Immutable
        public data object None : Labels

        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
        ) : Labels
    }

    @Immutable
    public data class Polygon(
        val strokeWidth: Dp,
        val strokeColorFallback: Color,
        val fillAlpha: Float,
    )

    @Immutable
    public sealed interface Vertices {
        @Immutable
        public data object None : Vertices

        @Immutable
        public data class Visible(
            val radius: Dp,
            val colorOverride: Color?,
        ) : Vertices
    }

    @Immutable
    public sealed interface Values {
        @Immutable
        public data object None : Values

        @Immutable
        public data class Visible(
            val textStyle: TextStyle,
            val formatter: (Float, RadarData) -> String,
            val offset: Dp,
        ) : Values
    }

    @Immutable
    public data class DataPolicy(
        val requireCompleteSeries: Boolean,
        val missingAsZero: Boolean,
    )
}