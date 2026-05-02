package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MoreVertical: ImageVector
    get() {
        if (_MoreVertical != null) {
            return _MoreVertical!!
        }
        _MoreVertical = ImageVector.Builder(
            name = "MoreVertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top dot
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 4f)
                curveTo(13.105f, 4f, 14f, 4.895f, 14f, 6f)
                curveTo(14f, 7.105f, 13.105f, 8f, 12f, 8f)
                curveTo(10.895f, 8f, 10f, 7.105f, 10f, 6f)
                curveTo(10f, 4.895f, 10.895f, 4f, 12f, 4f)
                close()
            }
            // Middle dot
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 10f)
                curveTo(13.105f, 10f, 14f, 10.895f, 14f, 12f)
                curveTo(14f, 13.105f, 13.105f, 14f, 12f, 14f)
                curveTo(10.895f, 14f, 10f, 13.105f, 10f, 12f)
                curveTo(10f, 10.895f, 10.895f, 10f, 12f, 10f)
                close()
            }
            // Bottom dot
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 16f)
                curveTo(13.105f, 16f, 14f, 16.895f, 14f, 18f)
                curveTo(14f, 19.105f, 13.105f, 20f, 12f, 20f)
                curveTo(10.895f, 20f, 10f, 19.105f, 10f, 18f)
                curveTo(10f, 16.895f, 10.895f, 16f, 12f, 16f)
                close()
            }
        }.build()

        return _MoreVertical!!
    }

@Suppress("ObjectPropertyName")
private var _MoreVertical: ImageVector? = null
