package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.OpenBook: ImageVector
    get() {
        if (_OpenBook != null) {
            return _OpenBook!!
        }
        _OpenBook = ImageVector.Builder(
            name = "OpenBook",
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
                moveTo(12f, 21f)
                verticalLineTo(7f)
                curveTo(12f, 5.895f, 12.895f, 5f, 14f, 5f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 5f, 22f, 5.269f, 22f, 5.6f)
                verticalLineTo(18.714f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 21f)
                verticalLineTo(7f)
                curveTo(12f, 5.895f, 11.105f, 5f, 10f, 5f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 5f, 2f, 5.269f, 2f, 5.6f)
                verticalLineTo(18.714f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14f, 19f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 19f)
                horizontalLineTo(2f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                curveTo(12f, 19.895f, 12.895f, 19f, 14f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                curveTo(12f, 19.895f, 11.105f, 19f, 10f, 19f)
            }
        }.build()

        return _OpenBook!!
    }

@Suppress("ObjectPropertyName")
private var _OpenBook: ImageVector? = null
