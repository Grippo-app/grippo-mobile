package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color

internal object AppColorTokens {

    object Common {
        val White = Color(0xFFFFFFFF)
        val Black = Color(0xFF000000)
        val Accent = Color(0xFF3366FF) // Общий акцент
    }

    object Semantic {
        val Error = Color(0xFFFF4D4F)
        val Warning = Color(0xFFFFAA2C)
        val Success = Color(0xFF3AC86B)
    }

    object Primary {
        val P100 = Color(0xFFE6ECFF)
        val P150 = Color(0xFFD4DFFF)
        val P200 = Color(0xFFC2D1FF)
        val P300 = Color(0xFF99B2FF)
        val P400 = Color(0xFF7090FF)
        val P500 = Color(0xFF3366FF)
        val P600 = Color(0xFF2B57E6)
        val P700 = Color(0xFF244ACC)
        val P800 = Color(0xFF1C3DAF)
        val P900 = Color(0xFF152F94)
    }

    object NeutralLight {
        val N100 = Color(0xFFF9FBFE)
        val N150 = Color(0xFFF6F9FF)
        val N200 = Color(0xFFE9EFF8)
        val N250 = Color(0xFFE1EAF6)
        val N300 = Color(0xFFC5D2E6)
        val N400 = Color(0xFFA7B8D1)
        val N500 = Color(0xFF8294B3)
        val N600 = Color(0xFF5F6C88)
        val N700 = Color(0xFF3E4961)
    }

    object NeutralDark {
        val N100 = Color(0xFF121212)
        val N150 = Color(0xFF181818)
        val N200 = Color(0xFF202020)
        val N250 = Color(0xFF252525)
        val N300 = Color(0xFF303030)
        val N400 = Color(0xFF404040)
        val N500 = Color(0xFF707070)
        val N600 = Color(0xFFA0A0A0)
        val N700 = Color(0xFFD0D0D0)
    }
}