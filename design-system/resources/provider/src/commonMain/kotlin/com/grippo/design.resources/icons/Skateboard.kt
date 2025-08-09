package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Skateboard: ImageVector
    get() {
        if (_Skateboard != null) {
            return _Skateboard!!
        }
        _Skateboard = ImageVector.Builder(
            name = "Skateboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 16f)
                curveTo(8.328f, 16f, 9f, 15.328f, 9f, 14.5f)
                curveTo(9f, 13.672f, 8.328f, 13f, 7.5f, 13f)
                curveTo(6.672f, 13f, 6f, 13.672f, 6f, 14.5f)
                curveTo(6f, 15.328f, 6.672f, 16f, 7.5f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9f)
                lineTo(6f, 10f)
                horizontalLineTo(18f)
                lineTo(21f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 16f)
                curveTo(17.328f, 16f, 18f, 15.328f, 18f, 14.5f)
                curveTo(18f, 13.672f, 17.328f, 13f, 16.5f, 13f)
                curveTo(15.672f, 13f, 15f, 13.672f, 15f, 14.5f)
                curveTo(15f, 15.328f, 15.672f, 16f, 16.5f, 16f)
                close()
            }
        }.build()

        return _Skateboard!!
    }

@Suppress("ObjectPropertyName")
private var _Skateboard: ImageVector? = null
