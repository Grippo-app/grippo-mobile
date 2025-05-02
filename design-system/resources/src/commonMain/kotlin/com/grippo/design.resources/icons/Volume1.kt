package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Volume1: ImageVector
    get() {
        if (_Volume1 != null) {
            return _Volume1!!
        }
        _Volume1 = ImageVector.Builder(
            name = "Volume1",
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
            }
        }.build()

        return _Volume1!!
    }

@Suppress("ObjectPropertyName")
private var _Volume1: ImageVector? = null
