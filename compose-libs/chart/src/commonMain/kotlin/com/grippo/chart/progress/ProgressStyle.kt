package com.grippo.chart.progress

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

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
    @Immutable
    public data class Layout(
        val barHeight: Dp,
        val spacing: Dp,
        val corner: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public sealed interface Domain {
        @Immutable
        public data object Normalized : Domain

        @Immutable
        public data class Absolute(
            val maxValue: Float?,
        ) : Domain
    }

    @Immutable
    public data class Bars(
        val trackColor: Color?,
        val brushProvider: ((ProgressChartData, Size, Rect) -> Brush)?,
        val strokeWidth: Dp,
        val strokeColor: Color,
    )

    @Immutable
    public data class Labels(
        val textStyle: TextStyle,
    )

    @Immutable
    public sealed interface Values {
        @Immutable
        public data object None : Values

        @Immutable
        public data class Inside(
            val textStyle: TextStyle,
            val formatter: (Float, ProgressData) -> String,
            val minInnerPadding: Dp,
            val insideColor: Color?,
            val preferNormalizedLabels: Boolean,
        ) : Values

        @Immutable
        public data class Outside(
            val textStyle: TextStyle,
            val formatter: (Float, ProgressData) -> String,
            val preferNormalizedLabels: Boolean,
        ) : Values
    }

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color,
        val width: Dp,
    )

    @Immutable
    public sealed interface Progression {
        @Immutable
        public data object Linear : Progression

        @Immutable
        public data class Power(val alpha: Float) : Progression

        @Immutable
        public data object Logarithmic : Progression
    }
}