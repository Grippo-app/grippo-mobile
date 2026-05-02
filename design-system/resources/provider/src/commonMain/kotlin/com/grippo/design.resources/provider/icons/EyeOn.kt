package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOn: ImageVector
    get() {
        if (_EyeOn != null) {
            return _EyeOn!!
        }
        _EyeOn = ImageVector.Builder(
            name = "EyeOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Wider, flatter almond eye (less circular) with hollow ring (iris cut-out).
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer almond — flatter top-bottom, wider left-right
                moveTo(1.5f, 12f)
                curveTo(3.5f, 7.5f, 7.5f, 5f, 12f, 5f)
                curveTo(16.5f, 5f, 20.5f, 7.5f, 22.5f, 12f)
                curveTo(20.5f, 16.5f, 16.5f, 19f, 12f, 19f)
                curveTo(7.5f, 19f, 3.5f, 16.5f, 1.5f, 12f)
                close()
                // Iris cut-out — bigger circle (~4 units radius)
                moveTo(12f, 8f)
                curveTo(14.209f, 8f, 16f, 9.791f, 16f, 12f)
                curveTo(16f, 14.209f, 14.209f, 16f, 12f, 16f)
                curveTo(9.791f, 16f, 8f, 14.209f, 8f, 12f)
                curveTo(8f, 9.791f, 9.791f, 8f, 12f, 8f)
                close()
            }
            // Inner pupil — solid dot in centre, sized so iris-ring is visible.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 9.5f)
                curveTo(13.381f, 9.5f, 14.5f, 10.619f, 14.5f, 12f)
                curveTo(14.5f, 13.381f, 13.381f, 14.5f, 12f, 14.5f)
                curveTo(10.619f, 14.5f, 9.5f, 13.381f, 9.5f, 12f)
                curveTo(9.5f, 10.619f, 10.619f, 9.5f, 12f, 9.5f)
                close()
            }
        }.build()

        return _EyeOn!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOn: ImageVector? = null
