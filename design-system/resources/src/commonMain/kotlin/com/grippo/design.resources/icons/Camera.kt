package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Camera: ImageVector
    get() {
        if (_Camera != null) {
            return _Camera!!
        }
        _Camera = ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 19f)
                curveTo(23f, 19.53f, 22.789f, 20.039f, 22.414f, 20.414f)
                curveTo(22.039f, 20.789f, 21.53f, 21f, 21f, 21f)
                horizontalLineTo(3f)
                curveTo(2.47f, 21f, 1.961f, 20.789f, 1.586f, 20.414f)
                curveTo(1.211f, 20.039f, 1f, 19.53f, 1f, 19f)
                verticalLineTo(8f)
                curveTo(1f, 7.47f, 1.211f, 6.961f, 1.586f, 6.586f)
                curveTo(1.961f, 6.211f, 2.47f, 6f, 3f, 6f)
                horizontalLineTo(7f)
                lineTo(9f, 3f)
                horizontalLineTo(15f)
                lineTo(17f, 6f)
                horizontalLineTo(21f)
                curveTo(21.53f, 6f, 22.039f, 6.211f, 22.414f, 6.586f)
                curveTo(22.789f, 6.961f, 23f, 7.47f, 23f, 8f)
                verticalLineTo(19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                curveTo(14.209f, 17f, 16f, 15.209f, 16f, 13f)
                curveTo(16f, 10.791f, 14.209f, 9f, 12f, 9f)
                curveTo(9.791f, 9f, 8f, 10.791f, 8f, 13f)
                curveTo(8f, 15.209f, 9.791f, 17f, 12f, 17f)
                close()
            }
        }.build()

        return _Camera!!
    }

@Suppress("ObjectPropertyName")
private var _Camera: ImageVector? = null
