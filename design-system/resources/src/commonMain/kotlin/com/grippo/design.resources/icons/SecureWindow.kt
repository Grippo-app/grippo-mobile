package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.SecureWindow: ImageVector
    get() {
        if (_SecureWindow != null) {
            return _SecureWindow!!
        }
        _SecureWindow = ImageVector.Builder(
            name = "SecureWindow",
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
                moveTo(13f, 21f)
                horizontalLineTo(4f)
                curveTo(2.895f, 21f, 2f, 20.105f, 2f, 19f)
                verticalLineTo(5f)
                curveTo(2f, 3.895f, 2.895f, 3f, 4f, 3f)
                horizontalLineTo(20f)
                curveTo(21.105f, 3f, 22f, 3.895f, 22f, 5f)
                verticalLineTo(12f)
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
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.992f, 14.125f)
                lineTo(21.548f, 14.774f)
                curveTo(21.814f, 14.842f, 22.001f, 15.083f, 21.993f, 15.358f)
                curveTo(21.821f, 21.116f, 18.5f, 22f, 18.5f, 22f)
                curveTo(18.5f, 22f, 15.179f, 21.116f, 15.007f, 15.358f)
                curveTo(14.999f, 15.083f, 15.186f, 14.842f, 15.452f, 14.774f)
                lineTo(18.008f, 14.125f)
                curveTo(18.331f, 14.043f, 18.669f, 14.043f, 18.992f, 14.125f)
                close()
            }
        }.build()

        return _SecureWindow!!
    }

@Suppress("ObjectPropertyName")
private var _SecureWindow: ImageVector? = null
