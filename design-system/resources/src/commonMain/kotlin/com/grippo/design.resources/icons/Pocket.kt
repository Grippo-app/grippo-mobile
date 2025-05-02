package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Pocket: ImageVector
    get() {
        if (_Pocket != null) {
            return _Pocket!!
        }
        _Pocket = ImageVector.Builder(
            name = "Pocket",
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
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                curveTo(20.53f, 3f, 21.039f, 3.211f, 21.414f, 3.586f)
                curveTo(21.789f, 3.961f, 22f, 4.47f, 22f, 5f)
                verticalLineTo(11f)
                curveTo(22f, 13.652f, 20.946f, 16.196f, 19.071f, 18.071f)
                curveTo(17.196f, 19.946f, 14.652f, 21f, 12f, 21f)
                curveTo(10.687f, 21f, 9.386f, 20.741f, 8.173f, 20.239f)
                curveTo(6.96f, 19.736f, 5.858f, 19f, 4.929f, 18.071f)
                curveTo(3.054f, 16.196f, 2f, 13.652f, 2f, 11f)
                verticalLineTo(5f)
                curveTo(2f, 4.47f, 2.211f, 3.961f, 2.586f, 3.586f)
                curveTo(2.961f, 3.211f, 3.47f, 3f, 4f, 3f)
                verticalLineTo(3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 10f)
                lineTo(12f, 14f)
                lineTo(16f, 10f)
            }
        }.build()

        return _Pocket!!
    }

@Suppress("ObjectPropertyName")
private var _Pocket: ImageVector? = null
