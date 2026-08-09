package com.example.design

import androidx.compose.ui.graphics.Color

object LightAppColors : AppColor {
    override val primary = AppPalette.error400
    override val background = AppPalette.neutral0
    override val error = object : AppColor.Error {
        override val content = AppPalette.error600
        override val container = Color(0xFFFFEBEE)
    }
    override val warning = object : AppColor.Warning {
        override val content = Color(0xFFFFA000)
    }
    override val extraGlow = Color(0xFF80DEEA)
}
