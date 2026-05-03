package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Flame: ImageVector
    get() {
        if (_Flame != null) {
            return _Flame!!
        }
        _Flame = ImageVector.Builder(
            name = "Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer flame: tear-drop with subtle right lean.
            // Inner flame cut-out via EvenOdd → readable two-tone shape.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Tip top, lean slightly right
                moveTo(12f, 2f)
                curveTo(14f, 5f, 18f, 8f, 18f, 13f)
                // Right side curving down to wide rounded base
                curveTo(18f, 18f, 15f, 22f, 12f, 22f)
                // Left base (symmetric round)
                curveTo(9f, 22f, 6f, 18f, 6f, 14f)
                // Left side narrower, with subtle inner shelf
                curveTo(6f, 11f, 8f, 9f, 10f, 8f)
                // Small "second tongue" shelf on the left
                curveTo(10f, 11f, 11f, 12f, 12f, 12f)
                // Back up to the tip
                curveTo(12f, 8f, 11f, 5f, 12f, 2f)
                close()
                // Inner flame cut-out (smaller flame inside the base)
                moveTo(12f, 14f)
                curveTo(13f, 14f, 14.5f, 15.5f, 14.5f, 17f)
                curveTo(14.5f, 19f, 13.328f, 20f, 12f, 20f)
                curveTo(10.672f, 20f, 9.5f, 19f, 9.5f, 17f)
                curveTo(9.5f, 16f, 10f, 15f, 10.5f, 14.5f)
                curveTo(10.7f, 15.2f, 11.2f, 15.5f, 11.5f, 15.5f)
                curveTo(11.5f, 15f, 11.7f, 14.5f, 12f, 14f)
                close()
            }
        }.build()

        return _Flame!!
    }

@Suppress("ObjectPropertyName")
private var _Flame: ImageVector? = null
