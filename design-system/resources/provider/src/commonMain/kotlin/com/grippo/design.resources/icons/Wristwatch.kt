package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Wristwatch: ImageVector
    get() {
        if (_Wristwatch != null) {
            return _Wristwatch!!
        }
        _Wristwatch = ImageVector.Builder(
            name = "Wristwatch",
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
                moveTo(16f, 16.472f)
                verticalLineTo(20f)
                curveTo(16f, 21.104f, 15.105f, 22f, 14f, 22f)
                horizontalLineTo(10f)
                curveTo(8.895f, 22f, 8f, 21.104f, 8f, 20f)
                verticalLineTo(16.472f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 7.528f)
                verticalLineTo(4f)
                curveTo(8f, 2.895f, 8.895f, 2f, 10f, 2f)
                horizontalLineTo(14f)
                curveTo(15.105f, 2f, 16f, 2.895f, 16f, 4f)
                verticalLineTo(7.528f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 12f)
                curveTo(18f, 8.686f, 15.314f, 6f, 12f, 6f)
                curveTo(8.686f, 6f, 6f, 8.686f, 6f, 12f)
                curveTo(6f, 15.314f, 8.686f, 18f, 12f, 18f)
                curveTo(15.314f, 18f, 18f, 15.314f, 18f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 12f)
                horizontalLineTo(12f)
                verticalLineTo(10f)
            }
        }.build()

        return _Wristwatch!!
    }

@Suppress("ObjectPropertyName")
private var _Wristwatch: ImageVector? = null
