package com.grippo.chart.utils

import androidx.compose.ui.graphics.Color

internal fun chooseContrastingText(bg: Color, defaultColor: Color): Color {
    // Simple luminance-based contrast heuristic
    val lum = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
    return if (lum < 0.5f) Color.White else Color.Black.copy(alpha = 0.85f)
}