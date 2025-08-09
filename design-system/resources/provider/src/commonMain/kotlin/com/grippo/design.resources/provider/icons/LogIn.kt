package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LogIn: ImageVector
    get() {
        if (_LogIn != null) {
            return _LogIn!!
        }
        _LogIn = ImageVector.Builder(
            name = "LogIn",
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
                moveTo(12f, 12f)
                lineTo(15f, 9f)
                moveTo(19f, 12f)
                horizontalLineTo(12f)
                horizontalLineTo(19f)
                close()
                moveTo(12f, 12f)
                lineTo(15f, 15f)
                lineTo(12f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 6f)
                verticalLineTo(5f)
                curveTo(19f, 3.895f, 18.105f, 3f, 17f, 3f)
                horizontalLineTo(7f)
                curveTo(5.895f, 3f, 5f, 3.895f, 5f, 5f)
                verticalLineTo(19f)
                curveTo(5f, 20.105f, 5.895f, 21f, 7f, 21f)
                horizontalLineTo(17f)
                curveTo(18.105f, 21f, 19f, 20.105f, 19f, 19f)
                verticalLineTo(18f)
            }
        }.build()

        return _LogIn!!
    }

@Suppress("ObjectPropertyName")
private var _LogIn: ImageVector? = null
