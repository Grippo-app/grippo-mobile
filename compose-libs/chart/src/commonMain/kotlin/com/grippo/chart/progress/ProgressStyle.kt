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
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
        val barHeight: Dp,
        val spacing: Dp,
        val corner: Dp,
        val labelPadding: Dp,
    )

    @Immutable
    public data class Domain(
        val normalized: Boolean,   // if true, values expected in 0..1
        val maxValue: Float?,      // optional manual max; ignored if <= 0
    )

    @Immutable
    public data class Bars(
        val trackColor: Color?,    // null disables track
        val brushProvider: ((ProgressChartData, Size, Rect) -> Brush)?,
        val strokeWidth: Dp,
        val strokeColor: Color,
    )

    @Immutable
    public data class Labels(
        val textStyle: TextStyle,
    )

    @Immutable
    public data class Values(
        val show: Boolean,
        val textStyle: TextStyle,
        val formatter: (Float, ProgressData) -> String,
        val placeInside: Boolean,
        val minInnerPadding: Dp,
        val insideColor: Color?,              // null -> auto contrast
        val preferNormalizedLabels: Boolean,  // when domain.normalized
    )

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color,
        val width: Dp,
    )
}