package com.grippo.chart.pie

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class PieStyle(
    val chartBarWidth: Dp = 22.dp,
    val paddingAngle: Float = 2f,
    val cornerRadius: Dp = 6.dp,
    val minVisibleAngle: Float = 6f,
    val textStyle: TextStyle,
)