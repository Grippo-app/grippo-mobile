package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color

internal object AppPalette {

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
        val N500 = Color(0xFF6A768C)
        val N550 = Color(0xFF7F8CA3)
        val N600 = Color(0xFFA1ADC3)
        val N700 = Color(0xFFBECADC)
        val N800 = Color(0xFFF4F7FA)
    }

    object Unique {
        // Red / Orange
        val Red = Color(0xFFFF4C4C)
        val Orange = Color(0xFFFF7A29)
        val Coral = Color(0xFFFF7F50)

        // Yellow/Amber
        val Amber = Color(0xFFFFA726)

        // Green / Teal
        val Green = Color(0xFF43A047)
        val Teal = Color(0xFF00897B)

        // Blue
        val Sky = Color(0xFF1E88E5)
        val Blue = Color(0xFF1565C0)
        val Navy = Color(0xFF283593)

        // Purple / Magenta
        val Purple = Color(0xFF7E57C2)
        val Violet = Color(0xFF8E24AA)
        val Magenta = Color(0xFFD81B60)

        // Brown / Warm
        val Brown = Color(0xFF6D4C41)
        val Burgundy = Color(0xFF880E4F)

        // Extra
        val Olive = Color(0xFF6B8E23)
        val Cyan = Color(0xFF0097A7)
        val Indigo = Color(0xFF3F51B5)
    }

    object Ramps {
        val BlueOrangeRed: List<Pair<Float, Color>> = listOf(
            0.00f to Primary.P400,
            0.16f to Primary.P300,
            0.36f to Color(0xFFFFE08A),
            0.56f to Color(0xFFFFB24D),
            0.76f to Color(0xFFFF7A33),
            0.90f to Color(0xFFFF5A2A),
            1.00f to Semantic.Error
        )

        val OrangeRed: List<Pair<Float, Color>> = listOf(
            0.00f to Color(0xFFFFE08A),
            0.30f to Color(0xFFFFB24D),
            0.60f to Color(0xFFFF7A33),
            0.85f to Color(0xFFFF5A2A),
            1.00f to Semantic.Error
        )
    }

    object Qualitative {
        val Palette9Blue: List<Color> = listOf(
            Primary.P300,
            Primary.P400,
            Primary.P500,
            Primary.P600,
            Primary.P700,
            Primary.P800,
            Primary.P900,
            Primary.P250,
            Primary.P100
        )

        val Palette20Colorful: List<Color> = listOf(
            Unique.Red,
            Unique.Orange,
            Unique.Coral,
            Unique.Amber,
            Unique.Green,
            Unique.Teal,
            Unique.Sky,
            Unique.Blue,
            Unique.Navy,
            Unique.Purple,
            Unique.Violet,
            Unique.Magenta,
            Unique.Brown,
            Unique.Burgundy,
            Unique.Olive,
            Unique.Cyan,
            Unique.Indigo,
            Common.Accent,
            Semantic.Success,
            Semantic.Error
        )
    }
}