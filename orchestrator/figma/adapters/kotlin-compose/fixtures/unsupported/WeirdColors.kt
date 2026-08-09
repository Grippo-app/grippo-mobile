package com.unsupported.design

import androidx.compose.ui.graphics.Color

object WeirdColors {
    val conditional = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val computed = computeColor()
    val named = Color.Red
    val lazyOne by lazy { Color(0xFF000000) }
    val viaGetter get() = Color(0xFF000000)
    val math = 8.dp * 2
    val dimensionish = 16.dp
}
