package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color

internal object AppColorTokens {

    object Common {
        val White = Color(0xFFFAFAFA)
        val Black = Color(0xFF121212)
        val Accent = Color(0xFF3366FF)
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
        val P250 = Color(0xFFB0C4FF)
        val P300 = Color(0xFF99B2FF)
        val P400 = Color(0xFF7090FF)
        val P500 = Color(0xFF3366FF)
        val P600 = Color(0xFF2B57E6)
        val P700 = Color(0xFF244ACC)
        val P800 = Color(0xFF1C3DAF)
        val P900 = Color(0xFF152F94)
    }

    object NeutralLight {
        val N050 = Color(0xFFFAFBFC)
        val N100 = Color(0xFFEDEFF1)
        val N150 = Color(0xFFE7E9ED)
        val N200 = Color(0xFFD9DDE4)
        val N250 = Color(0xFFCBD0D8)
        val N300 = Color(0xFFADB3BF)
        val N400 = Color(0xFF939AAA)
        val N500 = Color(0xFF798292)
        val N550 = Color(0xFF6B7484)
        val N600 = Color(0xFF606A7B)
        val N700 = Color(0xFF2F353F)
        val N800 = Color(0xFF15181F)
    }

    object NeutralDark {
        val N050 = Color(0xFF0F1115)
        val N100 = Color(0xFF181A1F)
        val N150 = Color(0xFF1F2228)
        val N200 = Color(0xFF262A31)
        val N250 = Color(0xFF2E333B)
        val N300 = Color(0xFF373D47)
        val N400 = Color(0xFF4C5566)
        val N500 = Color(0xFF6A768C) // secondary text
        val N550 = Color(0xFF7F8CA3) // between secondary text and icons
        val N600 = Color(0xFFA1ADC3) // icons
        val N700 = Color(0xFFBECADC) // primary text
        val N800 = Color(0xFFF4F7FA) // lightest text (near white)
    }
}