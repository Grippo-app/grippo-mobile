package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Skateboarding: ImageVector
    get() {
        if (_Skateboarding != null) {
            return _Skateboarding!!
        }
        _Skateboarding = ImageVector.Builder(
            name = "Skateboarding",
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
                moveTo(5f, 19f)
                lineTo(7.333f, 20f)
                horizontalLineTo(16.667f)
                lineTo(19f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 22.01f)
                lineTo(8.01f, 21.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 22.01f)
                lineTo(16.01f, 21.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7.833f)
                curveTo(7f, 7.833f, 8.828f, 6.919f, 10f, 6.333f)
                curveTo(12f, 5.333f, 14.271f, 6.901f, 14.271f, 6.901f)
                lineTo(9.962f, 10.036f)
                lineTo(14f, 13.333f)
                verticalLineTo(17.333f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.549f, 13.344f)
                lineTo(8.308f, 14.172f)
                horizontalLineTo(5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.165f, 9.209f)
                horizontalLineTo(17.887f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 6f)
                curveTo(18.105f, 6f, 19f, 5.105f, 19f, 4f)
                curveTo(19f, 2.895f, 18.105f, 2f, 17f, 2f)
                curveTo(15.895f, 2f, 15f, 2.895f, 15f, 4f)
                curveTo(15f, 5.105f, 15.895f, 6f, 17f, 6f)
                close()
            }
        }.build()

        return _Skateboarding!!
    }

@Suppress("ObjectPropertyName")
private var _Skateboarding: ImageVector? = null
