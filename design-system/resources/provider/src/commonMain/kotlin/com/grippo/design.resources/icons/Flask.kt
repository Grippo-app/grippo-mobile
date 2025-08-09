package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Flask: ImageVector
    get() {
        if (_Flask != null) {
            return _Flask!!
        }
        _Flask = ImageVector.Builder(
            name = "Flask",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 15f)
                horizontalLineTo(5.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 4f)
                horizontalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 4.5f)
                verticalLineTo(10.26f)
                curveTo(9f, 10.738f, 8.829f, 11.199f, 8.519f, 11.562f)
                lineTo(3.481f, 17.438f)
                curveTo(3.171f, 17.801f, 3f, 18.262f, 3f, 18.74f)
                verticalLineTo(19f)
                curveTo(3f, 20.105f, 3.895f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.105f, 21f, 21f, 20.105f, 21f, 19f)
                verticalLineTo(18.74f)
                curveTo(21f, 18.262f, 20.829f, 17.801f, 20.518f, 17.438f)
                lineTo(15.481f, 11.562f)
                curveTo(15.171f, 11.199f, 15f, 10.738f, 15f, 10.26f)
                verticalLineTo(4.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9.01f)
                lineTo(12.01f, 8.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 2.01f)
                lineTo(11.01f, 1.999f)
            }
        }.build()

        return _Flask!!
    }

@Suppress("ObjectPropertyName")
private var _Flask: ImageVector? = null
