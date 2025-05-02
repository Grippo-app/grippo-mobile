package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Image: ImageVector
    get() {
        if (_Image != null) {
            return _Image!!
        }
        _Image = ImageVector.Builder(
            name = "Image",
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
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.895f, 3f, 3f, 3.895f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.105f, 3.895f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.105f, 21f, 21f, 20.105f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.895f, 20.105f, 3f, 19f, 3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 10f)
                curveTo(9.328f, 10f, 10f, 9.328f, 10f, 8.5f)
                curveTo(10f, 7.672f, 9.328f, 7f, 8.5f, 7f)
                curveTo(7.672f, 7f, 7f, 7.672f, 7f, 8.5f)
                curveTo(7f, 9.328f, 7.672f, 10f, 8.5f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 15f)
                lineTo(16f, 10f)
                lineTo(5f, 21f)
            }
        }.build()

        return _Image!!
    }

@Suppress("ObjectPropertyName")
private var _Image: ImageVector? = null
