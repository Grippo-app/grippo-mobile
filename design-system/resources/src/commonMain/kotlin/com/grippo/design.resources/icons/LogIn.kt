package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

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
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 3f)
                horizontalLineTo(19f)
                curveTo(19.53f, 3f, 20.039f, 3.211f, 20.414f, 3.586f)
                curveTo(20.789f, 3.961f, 21f, 4.47f, 21f, 5f)
                verticalLineTo(19f)
                curveTo(21f, 19.53f, 20.789f, 20.039f, 20.414f, 20.414f)
                curveTo(20.039f, 20.789f, 19.53f, 21f, 19f, 21f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 17f)
                lineTo(15f, 12f)
                lineTo(10f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 12f)
                horizontalLineTo(3f)
            }
        }.build()

        return _LogIn!!
    }

@Suppress("ObjectPropertyName")
private var _LogIn: ImageVector? = null
