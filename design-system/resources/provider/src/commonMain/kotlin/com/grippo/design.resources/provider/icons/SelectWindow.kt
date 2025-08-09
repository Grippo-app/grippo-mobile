package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SelectWindow: ImageVector
    get() {
        if (_SelectWindow != null) {
            return _SelectWindow!!
        }
        _SelectWindow = ImageVector.Builder(
            name = "SelectWindow",
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
                moveTo(14f, 21f)
                horizontalLineTo(4f)
                curveTo(2.895f, 21f, 2f, 20.105f, 2f, 19f)
                verticalLineTo(5f)
                curveTo(2f, 3.895f, 2.895f, 3f, 4f, 3f)
                horizontalLineTo(20f)
                curveTo(21.105f, 3f, 22f, 3.895f, 22f, 5f)
                verticalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 7f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 5.01f)
                lineTo(5.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 5.01f)
                lineTo(8.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 5.01f)
                lineTo(11.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(22.082f, 18.365f)
                curveTo(22.576f, 18.669f, 22.546f, 19.408f, 22.037f, 19.466f)
                lineTo(19.471f, 19.757f)
                lineTo(18.319f, 22.069f)
                curveTo(18.091f, 22.527f, 17.386f, 22.303f, 17.27f, 21.735f)
                lineTo(16.014f, 15.619f)
                curveTo(15.916f, 15.139f, 16.347f, 14.837f, 16.764f, 15.094f)
                lineTo(22.082f, 18.365f)
                close()
            }
        }.build()

        return _SelectWindow!!
    }

@Suppress("ObjectPropertyName")
private var _SelectWindow: ImageVector? = null
