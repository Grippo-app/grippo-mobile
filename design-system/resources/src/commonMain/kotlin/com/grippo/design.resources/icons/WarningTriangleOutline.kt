package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.WarningTriangleOutline: ImageVector
    get() {
        if (_WarningTriangleOutline != null) {
            return _WarningTriangleOutline!!
        }
        _WarningTriangleOutline = ImageVector.Builder(
            name = "WarningTriangleOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.043f, 21f)
                horizontalLineTo(3.957f)
                curveTo(2.419f, 21f, 1.457f, 19.336f, 2.223f, 18.003f)
                lineTo(10.266f, 4.015f)
                curveTo(11.035f, 2.678f, 12.965f, 2.678f, 13.734f, 4.015f)
                lineTo(21.777f, 18.003f)
                curveTo(22.543f, 19.336f, 21.581f, 21f, 20.043f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17.01f)
                lineTo(12.01f, 16.999f)
            }
        }.build()

        return _WarningTriangleOutline!!
    }

@Suppress("ObjectPropertyName")
private var _WarningTriangleOutline: ImageVector? = null
