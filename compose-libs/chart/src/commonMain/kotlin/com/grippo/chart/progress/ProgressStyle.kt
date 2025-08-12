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
    // layout
    val padding: Dp = 12.dp,
    val barHeight: Dp = 16.dp,
    val spacing: Dp = 10.dp,
    val corner: Dp = 10.dp,
    val labelPadding: Dp = 6.dp,

    // values domain
    val normalized: Boolean = false,            // if true, values expected in 0..1
    val maxValue: Float? = null,                // optional manual max; ignored if <=0

    // bar visuals
    val trackColor: Color? = Color(0x14FFFFFF), // background behind each bar; null disables
    val barBrush: ((ProgressChartData, Size, Rect) -> Brush)? = null, // overrides data.color
    val barStrokeWidth: Dp = 0.dp,
    val barStrokeColor: Color = Color.Transparent,

    // labels
    val labelTextStyle: TextStyle = TextStyle(color = Color(0x77FFFFFF)),

    // value labels
    val showValue: Boolean = true,
    val valueTextStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
    val valueFormatter: (Float) -> String = { v ->
        if (normalized) "${(v * 100f).roundToInt()}%" else v.roundToInt().toString()
    },
    val placeValueInside: Boolean = true,
    val minInnerPadding: Dp = 6.dp,
    val valueInsideColor: Color? = null, // if null, auto-contrast with bar color/brush sample

    // optional target marker (vertical line)
    val targetValue: Float? = null,
    val targetColor: Color = Color(0x44FFFFFF),
    val targetWidth: Dp = 1.dp,
)