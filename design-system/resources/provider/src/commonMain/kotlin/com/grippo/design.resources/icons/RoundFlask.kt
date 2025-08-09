package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RoundFlask: ImageVector
    get() {
        if (_RoundFlask != null) {
            return _RoundFlask!!
        }
        _RoundFlask = ImageVector.Builder(
            name = "RoundFlask",
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
                moveTo(19f, 15f)
                horizontalLineTo(5f)
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
                moveTo(15f, 4.5f)
                verticalLineTo(8.753f)
                curveTo(15f, 9.516f, 15.445f, 10.198f, 16.078f, 10.624f)
                curveTo(18.287f, 12.109f, 20f, 14.617f, 20f, 17.462f)
                curveTo(20f, 18.274f, 19.886f, 19.059f, 19.675f, 19.8f)
                curveTo(19.46f, 20.551f, 18.73f, 21f, 17.949f, 21f)
                horizontalLineTo(6.051f)
                curveTo(5.27f, 21f, 4.54f, 20.551f, 4.325f, 19.8f)
                curveTo(4.114f, 19.059f, 4f, 18.274f, 4f, 17.462f)
                curveTo(4f, 14.617f, 5.713f, 12.109f, 7.922f, 10.624f)
                curveTo(8.555f, 10.198f, 9f, 9.516f, 9f, 8.753f)
                verticalLineTo(4.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 7.01f)
                lineTo(13.01f, 6.999f)
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

        return _RoundFlask!!
    }

@Suppress("ObjectPropertyName")
private var _RoundFlask: ImageVector? = null
