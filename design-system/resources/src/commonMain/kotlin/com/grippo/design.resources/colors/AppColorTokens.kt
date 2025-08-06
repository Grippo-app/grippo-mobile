package com.grippo.design.resources.colors

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
        val N050 = Color(0xFFF9FAFB) // screen background
        val N100 = Color(0xFFF2F4F6) // card background
        val N150 = Color(0xFFEDEFF2) // container background
        val N200 = Color(0xFFDDE1E7) // border default
        val N250 = Color(0xFFCED3DB) // divider
        val N300 = Color(0xFFAEB5C2) // disabled text, skeleton
        val N400 = Color(0xFF949CAD) // tertiary text
        val N500 = Color(0xFF788191) // secondary text
        val N550 = Color(0xFF6A7382) // между secondary text и icons
        val N600 = Color(0xFF5F687A) // icons
        val N700 = Color(0xFF2F343F) // primary text
        val N800 = Color(0xFF14171E) // darker text
    }

    object NeutralDark {
        val N050 = Color(0xFF0F1115) // screen background (deepest)
        val N100 = Color(0xFF181A1F) // base background
        val N150 = Color(0xFF1F2228) // card background
        val N200 = Color(0xFF262A31) // container background
        val N250 = Color(0xFF2E333B) // border default
        val N300 = Color(0xFF373D47) // divider, elevated surfaces
        val N400 = Color(0xFF4C5566) // tertiary text
        val N500 = Color(0xFF6A768C) // secondary text
        val N550 = Color(0xFF7F8CA3) // between secondary text and icons
        val N600 = Color(0xFFA1ADC3) // icons
        val N700 = Color(0xFFBECADC) // primary text
        val N800 = Color(0xFFF4F7FA) // lightest text (near white)
    }
}