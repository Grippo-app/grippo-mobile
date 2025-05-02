package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Volume2: ImageVector
    get() {
        if (_Volume2 != null) {
            return _Volume2!!
        }
        _Volume2 = ImageVector.Builder(
            name = "Volume2",
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
                moveTo(11f, 5f)
                lineTo(6f, 9f)
                horizontalLineTo(2f)
                verticalLineTo(15f)
                horizontalLineTo(6f)
                lineTo(11f, 19f)
                verticalLineTo(5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.54f, 8.46f)
                curveTo(16.477f, 9.398f, 17.004f, 10.669f, 17.004f, 11.995f)
                curveTo(17.004f, 13.321f, 16.477f, 14.592f, 15.54f, 15.53f)
                moveTo(19.07f, 4.93f)
                curveTo(20.945f, 6.805f, 21.998f, 9.348f, 21.998f, 12f)
                curveTo(21.998f, 14.652f, 20.945f, 17.195f, 19.07f, 19.07f)
                verticalLineTo(4.93f)
                close()
            }
        }.build()

        return _Volume2!!
    }

@Suppress("ObjectPropertyName")
private var _Volume2: ImageVector? = null
