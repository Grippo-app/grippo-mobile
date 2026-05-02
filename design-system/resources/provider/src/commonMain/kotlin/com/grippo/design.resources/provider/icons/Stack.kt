package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Stack: ImageVector
    get() {
        if (_Stack != null) {
            return _Stack!!
        }
        _Stack = ImageVector.Builder(
            name = "Stack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top slab — pill-shaped (fully rounded ends)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(6.25f, 4f)
                horizontalLineTo(17.75f)
                curveTo(19.0f, 4f, 20f, 5.0f, 20f, 6.25f)
                curveTo(20f, 7.5f, 19f, 8.5f, 17.75f, 8.5f)
                horizontalLineTo(6.25f)
                curveTo(5f, 8.5f, 4f, 7.5f, 4f, 6.25f)
                curveTo(4f, 5f, 5f, 4f, 6.25f, 4f)
                close()
            }
            // Middle slab — pill-shaped
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(6.25f, 9.75f)
                horizontalLineTo(17.75f)
                curveTo(19f, 9.75f, 20f, 10.75f, 20f, 12f)
                curveTo(20f, 13.25f, 19f, 14.25f, 17.75f, 14.25f)
                horizontalLineTo(6.25f)
                curveTo(5f, 14.25f, 4f, 13.25f, 4f, 12f)
                curveTo(4f, 10.75f, 5f, 9.75f, 6.25f, 9.75f)
                close()
            }
            // Bottom slab — pill-shaped
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(6.25f, 15.5f)
                horizontalLineTo(17.75f)
                curveTo(19f, 15.5f, 20f, 16.5f, 20f, 17.75f)
                curveTo(20f, 19f, 19f, 20f, 17.75f, 20f)
                horizontalLineTo(6.25f)
                curveTo(5f, 20f, 4f, 19f, 4f, 17.75f)
                curveTo(4f, 16.5f, 5f, 15.5f, 6.25f, 15.5f)
                close()
            }
        }.build()

        return _Stack!!
    }

@Suppress("ObjectPropertyName")
private var _Stack: ImageVector? = null
