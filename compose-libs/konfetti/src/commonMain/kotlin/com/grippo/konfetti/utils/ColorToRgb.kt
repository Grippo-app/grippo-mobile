package com.grippo.konfetti.utils

import androidx.compose.ui.graphics.Color

internal fun Color.toArgb(): Int {
    return (alpha * 255).toInt().shl(24) or
            (red * 255).toInt().shl(16) or
            (green * 255).toInt().shl(8) or
            (blue * 255).toInt()
}