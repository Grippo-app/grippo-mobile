package com.grippo.chart.progress

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
public data class ProgressStyle(
    val layout: Layout = Layout(),
    val domain: Domain = Domain(),
    val bars: Bars = Bars(),
    val labels: Labels = Labels(),
    val values: Values = Values(),
    val target: Target? = null,
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 12.dp,
        val barHeight: Dp = 16.dp,
        val spacing: Dp = 10.dp,
        val corner: Dp = 10.dp,
        val labelPadding: Dp = 6.dp,
    )

    @Immutable
    public data class Domain(
        val normalized: Boolean = false,   // if true, values expected in 0..1
        val maxValue: Float? = null,       // optional manual max; ignored if <= 0
    )

    @Immutable
    public data class Bars(
        val trackColor: Color? = Color(0x14FFFFFF),
        val brushProvider: ((ProgressChartData, Size, Rect) -> Brush)? = null, // overrides data.color
        val strokeWidth: Dp = 0.dp,
        val strokeColor: Color = Color.Transparent,
    )

    @Immutable
    public data class Labels(
        val textStyle: TextStyle = TextStyle(color = Color(0x77FFFFFF)),
    )

    @Immutable
    public data class Values(
        val show: Boolean = true,
        val textStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        val formatter: (Float, ProgressData) -> String = { v, d ->
            d.valueUnit?.let { "${v.roundToInt()} ${it}" } ?: v.roundToInt().toString()
        },
        val placeInside: Boolean = true,
        val minInnerPadding: Dp = 6.dp,
        val insideColor: Color? = null, // null -> auto contrast
        val preferNormalizedLabels: Boolean = true, // when domain.normalized
    )

    @Immutable
    public data class Target(
        val value: Float,
        val color: Color = Color(0x44FFFFFF),
        val width: Dp = 1.dp,
    )
}