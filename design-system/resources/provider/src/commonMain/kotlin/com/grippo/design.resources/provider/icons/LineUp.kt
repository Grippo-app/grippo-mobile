package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LineUp: ImageVector
    get() {
        if (_LineUp != null) {
            return _LineUp!!
        }
        _LineUp = ImageVector.Builder(
            name = "LineUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 6f)
                lineTo(15.707f, 11.293f)
                curveTo(15.317f, 11.683f, 14.683f, 11.683f, 14.293f, 11.293f)
                lineTo(12.707f, 9.707f)
                curveTo(12.317f, 9.317f, 11.683f, 9.317f, 11.293f, 9.707f)
                lineTo(7f, 14f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 3f)
                verticalLineTo(17.8f)
                curveTo(3f, 18.92f, 3f, 19.48f, 3.218f, 19.908f)
                curveTo(3.41f, 20.284f, 3.716f, 20.59f, 4.092f, 20.782f)
                curveTo(4.52f, 21f, 5.08f, 21f, 6.2f, 21f)
                horizontalLineTo(21f)
            }
        }.build()

        return _LineUp!!
    }

@Suppress("ObjectPropertyName")
private var _LineUp: ImageVector? = null
