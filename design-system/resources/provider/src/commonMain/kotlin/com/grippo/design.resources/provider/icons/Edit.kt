package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Edit: ImageVector
    get() {
        if (_Edit != null) {
            return _Edit!!
        }
        _Edit = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Pencil shaft (slanted parallelogram, ~3.5 units thick).
            // Top edge: (16, 5) → (19, 8). Bottom edge: (5, 16) → (8, 19).
            // Cap matches the same width.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(16f, 5f)
                lineTo(19f, 8f)
                lineTo(8f, 19f)
                lineTo(5f, 16f)
                close()
            }
            // Cap — same diagonal slab, sits on top of the shaft (upper-right).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(17f, 4f)
                curveTo(17.828f, 3.172f, 19.172f, 3.172f, 20f, 4f)
                lineTo(20f, 4f)
                curveTo(20.828f, 4.828f, 20.828f, 6.172f, 20f, 7f)
                lineTo(19f, 8f)
                lineTo(16f, 5f)
                close()
            }
            // Tip — small filled triangle pointing lower-left, aligned to shaft width.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(4.5f, 16.5f)
                lineTo(7.5f, 19.5f)
                lineTo(3.5f, 21.2f)
                curveTo(3.1f, 21.4f, 2.6f, 20.9f, 2.8f, 20.5f)
                close()
            }
        }.build()

        return _Edit!!
    }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null
