package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color

internal object AppPalette {

    object Common {
        val White = Color(0xFFFAFAFA)
        val Black = Color(0xFF0D0F12)
    }

    object NeutralDark {
        val N050 = Color(0xFF0D0F12) // base background
        val N100 = Color(0xFF12151A) // surface-0
        val N150 = Color(0xFF171B21) // surface-1
        val N200 = Color(0xFF1C2128) // surface-2
        val N250 = Color(0xFF222833) // surface-3
        val N300 = Color(0xFF2A303B) // surface-4
        val N400 = Color(0xFF333B47) // outline/low-contrast text
        val N500 = Color(0xFF4A5566) // muted text/icons
        val N550 = Color(0xFF5B667A)
        val N600 = Color(0xFF7D8898)
        val N700 = Color(0xFFA5B0BF)
        val N800 = Color(0xFFF4F7FA) // high-contrast on dark kept as before
    }

    object Blue {
        val P300 = Color(0xFF99B2FF)
        val P400 = Color(0xFF7090FF)
        val P500 = Color(0xFF3366FF)
        val P600 = Color(0xFF2B57E6)
        val P700 = Color(0xFF244ACC)
        val P800 = Color(0xFF1C3DAF)
        val P900 = Color(0xFF152F94)
    }

    object Unique {
        // Warm (Red / Orange / Amber)
        val Red = Color(0xFFFF4C4C)
        val Orange = Color(0xFFFF7A29)
        val Coral = Color(0xFFFF7F50)
        val Amber = Color(0xFFF57F17)

        // Green / Teal / Olive
        val Green = Color(0xFF43A047)
        val Emerald = Color(0xFF2E7D32)
        val Teal = Color(0xFF00897B)
        val Olive = Color(0xFF6B8E23)

        // Blue / Indigo / Cyan
        val Sky = Color(0xFF1E88E5)
        val Blue = Color(0xFF1565C0)
        val Indigo = Color(0xFF3F51B5)
        val Cyan = Color(0xFF0097A7)

        // Purple / Magenta
        val Purple = Color(0xFF7E57C2)
        val Violet = Color(0xFF8E24AA)
        val Magenta = Color(0xFFD81B60)
        val Burgundy = Color(0xFF880E4F)

        // Brown / Earth
        val Brown = Color(0xFF6D4C41)
        val Cocoa = Color(0xFF5D4037)
        val Copper = Color(0xFF8D6E63)
        val Clay = Color(0xFFA1887F)
    }

    object Gradient {
        val Palette7BlueGrowth: List<Color> = listOf(
            Blue.P300,
            Blue.P400,
            Blue.P500,
            Blue.P600,
            Blue.P700,
            Blue.P800,
            Blue.P900,
        )

        val Palette5OrangeRedGrowth: List<Color> = listOf(
            Unique.Amber,     // light warm yellow-orange
            Unique.Coral,     // soft orange-red
            Unique.Orange,    // saturated orange
            Unique.Red,       // bright red
            Unique.Burgundy   // deep dark red
        )

        val Palette6MuscleCalm: List<Color> = listOf(
            Unique.Clay,     // warm neutral
            Unique.Copper,   // rosy copper
            Unique.Brown,    // earthy brown
            Unique.Cocoa,    // deep cocoa
            Unique.Burgundy, // muted burgundy
            Unique.Purple,   // soft purple accent
        )
    }
}
