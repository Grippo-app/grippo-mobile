package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOff: ImageVector
    get() {
        if (_EyeOff != null) {
            return _EyeOff!!
        }
        _EyeOff = ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Same almond + iris ring shape as EyeOn.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(1.5f, 12f)
                curveTo(3.5f, 7.5f, 7.5f, 5f, 12f, 5f)
                curveTo(16.5f, 5f, 20.5f, 7.5f, 22.5f, 12f)
                curveTo(20.5f, 16.5f, 16.5f, 19f, 12f, 19f)
                curveTo(7.5f, 19f, 3.5f, 16.5f, 1.5f, 12f)
                close()
                moveTo(12f, 8f)
                curveTo(14.209f, 8f, 16f, 9.791f, 16f, 12f)
                curveTo(16f, 14.209f, 14.209f, 16f, 12f, 16f)
                curveTo(9.791f, 16f, 8f, 14.209f, 8f, 12f)
                curveTo(8f, 9.791f, 9.791f, 8f, 12f, 8f)
                close()
            }
            // Solid pupil.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 9.5f)
                curveTo(13.381f, 9.5f, 14.5f, 10.619f, 14.5f, 12f)
                curveTo(14.5f, 13.381f, 13.381f, 14.5f, 12f, 14.5f)
                curveTo(10.619f, 14.5f, 9.5f, 13.381f, 9.5f, 12f)
                curveTo(9.5f, 10.619f, 10.619f, 9.5f, 12f, 9.5f)
                close()
            }
            // Diagonal slash — thick rounded bar across.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(3.3f, 4.7f)
                curveTo(3.7f, 4.3f, 4.3f, 4.3f, 4.7f, 4.7f)
                lineTo(20.3f, 20.3f)
                curveTo(20.7f, 20.7f, 20.7f, 21.3f, 20.3f, 21.7f)
                curveTo(19.9f, 22.1f, 19.3f, 22.1f, 18.9f, 21.7f)
                lineTo(3.3f, 6.1f)
                curveTo(2.9f, 5.7f, 2.9f, 5.1f, 3.3f, 4.7f)
                close()
            }
        }.build()

        return _EyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOff: ImageVector? = null
