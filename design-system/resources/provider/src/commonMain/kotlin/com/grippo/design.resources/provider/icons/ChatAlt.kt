package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ChatAlt: ImageVector
    get() {
        if (_ChatAlt != null) {
            return _ChatAlt!!
        }
        _ChatAlt = ImageVector.Builder(
            name = "ChatAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Speech bubble with soft tail and pill-shaped message lines cut out.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer bubble shape with tail
                moveTo(6f, 3f)
                horizontalLineTo(18f)
                curveTo(19.657f, 3f, 21f, 4.343f, 21f, 6f)
                verticalLineTo(14f)
                curveTo(21f, 15.657f, 19.657f, 17f, 18f, 17f)
                horizontalLineTo(12.5f)
                lineTo(7.7f, 21.2f)
                curveTo(7.0f, 21.8f, 6f, 21.3f, 6f, 20.4f)
                verticalLineTo(17f)
                curveTo(4.343f, 17f, 3f, 15.657f, 3f, 14f)
                verticalLineTo(6f)
                curveTo(3f, 4.343f, 4.343f, 3f, 6f, 3f)
                close()
                // Top message line cut-out (pill)
                moveTo(7f, 8f)
                curveTo(7f, 7.448f, 7.448f, 7f, 8f, 7f)
                horizontalLineTo(16f)
                curveTo(16.552f, 7f, 17f, 7.448f, 17f, 8f)
                curveTo(17f, 8.552f, 16.552f, 9f, 16f, 9f)
                horizontalLineTo(8f)
                curveTo(7.448f, 9f, 7f, 8.552f, 7f, 8f)
                close()
                // Bottom message line cut-out (shorter pill)
                moveTo(7f, 12f)
                curveTo(7f, 11.448f, 7.448f, 11f, 8f, 11f)
                horizontalLineTo(13f)
                curveTo(13.552f, 11f, 14f, 11.448f, 14f, 12f)
                curveTo(14f, 12.552f, 13.552f, 13f, 13f, 13f)
                horizontalLineTo(8f)
                curveTo(7.448f, 13f, 7f, 12.552f, 7f, 12f)
                close()
            }
        }.build()

        return _ChatAlt!!
    }

@Suppress("ObjectPropertyName")
private var _ChatAlt: ImageVector? = null
