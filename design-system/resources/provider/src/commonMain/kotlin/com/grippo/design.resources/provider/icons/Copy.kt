package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Copy: ImageVector
    get() {
        if (_Copy != null) {
            return _Copy!!
        }
        _Copy = ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Back document — rounded "L" frame implied by EvenOdd cut-out.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer back rectangle (all four corners rounded)
                moveTo(8f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(16f)
                curveTo(21f, 17.105f, 20.105f, 18f, 19f, 18f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                curveTo(17f, 8.343f, 15.657f, 7f, 14f, 7f)
                horizontalLineTo(6f)
                verticalLineTo(5f)
                curveTo(6f, 3.895f, 6.895f, 3f, 8f, 3f)
                close()
            }
            // Front document — fully rounded rectangle
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(5f, 8f)
                horizontalLineTo(14f)
                curveTo(15.105f, 8f, 16f, 8.895f, 16f, 10f)
                verticalLineTo(19f)
                curveTo(16f, 20.105f, 15.105f, 21f, 14f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                verticalLineTo(10f)
                curveTo(3f, 8.895f, 3.895f, 8f, 5f, 8f)
                close()
            }
        }.build()

        return _Copy!!
    }

@Suppress("ObjectPropertyName")
private var _Copy: ImageVector? = null
