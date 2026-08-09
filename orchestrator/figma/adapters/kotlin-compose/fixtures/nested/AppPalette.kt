package com.example.design

import androidx.compose.ui.graphics.Color

object AppPalette {
    val error400 = Color(0xFFEF5350)
    val error600 = Color(0xFFE53935)
    val neutral0 = Color(0xFFFFFFFF)
    val neutral900 = Color(0xFF111111)
    val overlayScrim = neutral900.copy(alpha = 0.4f)

    object Brand {
        val accent = Color(0xFF00E5FF)
    }
}
