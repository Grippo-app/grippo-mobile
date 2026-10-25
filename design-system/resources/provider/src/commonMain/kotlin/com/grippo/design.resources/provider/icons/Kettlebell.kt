package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Kettlebell: ImageVector
    get() {
        if (_Kettlebell != null) {
            return _Kettlebell!!
        }
        _Kettlebell = ImageVector.Builder(
            name = "Kettlebell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 7f)
                curveTo(16.418f, 7f, 20f, 10.582f, 20f, 15f)
                curveTo(20f, 19.418f, 16.418f, 23f, 12f, 23f)
                curveTo(7.582f, 23f, 4f, 19.418f, 4f, 15f)
                curveTo(4f, 10.582f, 7.582f, 7f, 12f, 7f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7f, 9f)
                verticalLineTo(6f)
                curveTo(7f, 3.239f, 9.239f, 1f, 12f, 1f)
                curveTo(14.761f, 1f, 17f, 3.239f, 17f, 6f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                close()
                moveTo(9f, 9f)
                verticalLineTo(6f)
                curveTo(9f, 4.343f, 10.343f, 3f, 12f, 3f)
                curveTo(13.657f, 3f, 15f, 4.343f, 15f, 6f)
                verticalLineTo(9f)
                horizontalLineTo(9f)
                close()
            }
        }.build()

        return _Kettlebell!!
    }

@Suppress("ObjectPropertyName")
private var _Kettlebell: ImageVector? = null
