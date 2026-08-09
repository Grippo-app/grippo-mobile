package com.example.design

import androidx.compose.ui.graphics.Color

object DarkAppColors : AppColor {
    override val primary = AppPalette.error600
    override val background = AppPalette.neutral900
    override val error = object : AppColor.Error {
        override val content = AppPalette.error400
    }
    override val darkOnly = Color(0xFF010203)
}
