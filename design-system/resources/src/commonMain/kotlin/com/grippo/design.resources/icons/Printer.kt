package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Printer: ImageVector
    get() {
        if (_Printer != null) {
            return _Printer!!
        }
        _Printer = ImageVector.Builder(
            name = "Printer",
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
                moveTo(6f, 9f)
                verticalLineTo(2f)
                horizontalLineTo(18f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18f)
                horizontalLineTo(4f)
                curveTo(3.47f, 18f, 2.961f, 17.789f, 2.586f, 17.414f)
                curveTo(2.211f, 17.039f, 2f, 16.53f, 2f, 16f)
                verticalLineTo(11f)
                curveTo(2f, 10.47f, 2.211f, 9.961f, 2.586f, 9.586f)
                curveTo(2.961f, 9.211f, 3.47f, 9f, 4f, 9f)
                horizontalLineTo(20f)
                curveTo(20.53f, 9f, 21.039f, 9.211f, 21.414f, 9.586f)
                curveTo(21.789f, 9.961f, 22f, 10.47f, 22f, 11f)
                verticalLineTo(16f)
                curveTo(22f, 16.53f, 21.789f, 17.039f, 21.414f, 17.414f)
                curveTo(21.039f, 17.789f, 20.53f, 18f, 20f, 18f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 14f)
                horizontalLineTo(6f)
                verticalLineTo(22f)
                horizontalLineTo(18f)
                verticalLineTo(14f)
                close()
            }
        }.build()

        return _Printer!!
    }

@Suppress("ObjectPropertyName")
private var _Printer: ImageVector? = null
