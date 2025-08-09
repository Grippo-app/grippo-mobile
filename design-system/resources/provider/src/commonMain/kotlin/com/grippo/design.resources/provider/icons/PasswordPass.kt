package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PasswordPass: ImageVector
    get() {
        if (_PasswordPass != null) {
            return _PasswordPass!!
        }
        _PasswordPass = ImageVector.Builder(
            name = "PasswordPass",
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
                moveTo(21f, 13f)
                verticalLineTo(8f)
                curveTo(21f, 6.895f, 20.105f, 6f, 19f, 6f)
                horizontalLineTo(5f)
                curveTo(3.895f, 6f, 3f, 6.895f, 3f, 8f)
                verticalLineTo(14f)
                curveTo(3f, 15.105f, 3.895f, 16f, 5f, 16f)
                horizontalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 18.5f)
                lineTo(16.5f, 20.5f)
                lineTo(20.5f, 16.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11.01f)
                lineTo(12.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 11.01f)
                lineTo(16.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 11.01f)
                lineTo(8.01f, 10.999f)
            }
        }.build()

        return _PasswordPass!!
    }

@Suppress("ObjectPropertyName")
private var _PasswordPass: ImageVector? = null
