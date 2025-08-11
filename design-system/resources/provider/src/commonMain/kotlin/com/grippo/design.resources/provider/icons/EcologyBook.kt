package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EcologyBook: ImageVector
    get() {
        if (_EcologyBook != null) {
            return _EcologyBook!!
        }
        _EcologyBook = ImageVector.Builder(
            name = "EcologyBook",
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
                moveTo(4f, 19f)
                verticalLineTo(5f)
                curveTo(4f, 3.895f, 4.895f, 3f, 6f, 3f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 3f, 20f, 3.269f, 20f, 3.6f)
                verticalLineTo(16.714f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 14f)
                curveTo(10f, 14f, 10.9f, 10.882f, 13f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.802f, 12.425f)
                lineTo(12.668f, 12.437f)
                curveTo(10.976f, 12.596f, 9.469f, 11.354f, 9.302f, 9.663f)
                curveTo(9.135f, 7.972f, 10.372f, 6.472f, 12.064f, 6.313f)
                lineTo(15.049f, 6.033f)
                curveTo(15.241f, 6.015f, 15.411f, 6.155f, 15.43f, 6.347f)
                lineTo(15.685f, 8.928f)
                curveTo(15.859f, 10.693f, 14.568f, 12.259f, 12.802f, 12.425f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 17f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 21f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                curveTo(4.895f, 21f, 4f, 20.105f, 4f, 19f)
                curveTo(4f, 17.895f, 4.895f, 17f, 6f, 17f)
            }
        }.build()

        return _EcologyBook!!
    }

@Suppress("ObjectPropertyName")
private var _EcologyBook: ImageVector? = null
