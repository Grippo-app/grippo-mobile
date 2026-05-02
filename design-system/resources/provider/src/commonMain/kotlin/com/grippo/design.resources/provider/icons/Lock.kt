package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Lock: ImageVector
    get() {
        if (_Lock != null) {
            return _Lock!!
        }
        _Lock = ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Shackle (top arch) — outline-only via stroke would break filled goal,
            // so represent shackle as a filled arc (donut half) using EvenOdd.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer arch
                moveTo(7f, 11f)
                verticalLineTo(8f)
                curveTo(7f, 5.239f, 9.239f, 3f, 12f, 3f)
                curveTo(14.761f, 3f, 17f, 5.239f, 17f, 8f)
                verticalLineTo(11f)
                horizontalLineTo(15f)
                verticalLineTo(8f)
                curveTo(15f, 6.343f, 13.657f, 5f, 12f, 5f)
                curveTo(10.343f, 5f, 9f, 6.343f, 9f, 8f)
                verticalLineTo(11f)
                close()
            }
            // Body
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(5f, 11f)
                horizontalLineTo(19f)
                curveTo(20.105f, 11f, 21f, 11.895f, 21f, 13f)
                verticalLineTo(20f)
                curveTo(21f, 21.105f, 20.105f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.895f, 22f, 3f, 21.105f, 3f, 20f)
                verticalLineTo(13f)
                curveTo(3f, 11.895f, 3.895f, 11f, 5f, 11f)
                close()
            }
        }.build()

        return _Lock!!
    }

@Suppress("ObjectPropertyName")
private var _Lock: ImageVector? = null
