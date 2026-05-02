package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Sparkle: ImageVector
    get() {
        if (_Sparkle != null) {
            return _Sparkle!!
        }
        _Sparkle = ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Big 4-point star
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 2f)
                curveTo(12.5f, 7.5f, 14.5f, 9.5f, 20f, 10f)
                curveTo(14.5f, 10.5f, 12.5f, 12.5f, 12f, 18f)
                curveTo(11.5f, 12.5f, 9.5f, 10.5f, 4f, 10f)
                curveTo(9.5f, 9.5f, 11.5f, 7.5f, 12f, 2f)
                close()
            }
            // Small accent star (lower-right)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(18f, 16f)
                curveTo(18.2f, 18.2f, 18.8f, 18.8f, 21f, 19f)
                curveTo(18.8f, 19.2f, 18.2f, 19.8f, 18f, 22f)
                curveTo(17.8f, 19.8f, 17.2f, 19.2f, 15f, 19f)
                curveTo(17.2f, 18.8f, 17.8f, 18.2f, 18f, 16f)
                close()
            }
        }.build()

        return _Sparkle!!
    }

@Suppress("ObjectPropertyName")
private var _Sparkle: ImageVector? = null
