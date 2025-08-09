package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SimpleCart: ImageVector
    get() {
        if (_SimpleCart != null) {
            return _SimpleCart!!
        }
        _SimpleCart = ImageVector.Builder(
            name = "SimpleCart",
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
                moveTo(3f, 6f)
                lineTo(2.25f, 3.5f)
                moveTo(3f, 6f)
                horizontalLineTo(22f)
                lineTo(19f, 16f)
                horizontalLineTo(6f)
                lineTo(3f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 19.5f)
                curveTo(11f, 20.328f, 10.328f, 21f, 9.5f, 21f)
                curveTo(8.672f, 21f, 8f, 20.328f, 8f, 19.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 19.5f)
                curveTo(17f, 20.328f, 16.328f, 21f, 15.5f, 21f)
                curveTo(14.672f, 21f, 14f, 20.328f, 14f, 19.5f)
            }
        }.build()

        return _SimpleCart!!
    }

@Suppress("ObjectPropertyName")
private var _SimpleCart: ImageVector? = null
