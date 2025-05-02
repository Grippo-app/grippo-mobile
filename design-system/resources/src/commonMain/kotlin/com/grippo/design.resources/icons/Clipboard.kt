package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Clipboard: ImageVector
    get() {
        if (_Clipboard != null) {
            return _Clipboard!!
        }
        _Clipboard = ImageVector.Builder(
            name = "Clipboard",
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
                moveTo(16f, 4f)
                horizontalLineTo(18f)
                curveTo(18.53f, 4f, 19.039f, 4.211f, 19.414f, 4.586f)
                curveTo(19.789f, 4.961f, 20f, 5.47f, 20f, 6f)
                verticalLineTo(20f)
                curveTo(20f, 20.53f, 19.789f, 21.039f, 19.414f, 21.414f)
                curveTo(19.039f, 21.789f, 18.53f, 22f, 18f, 22f)
                horizontalLineTo(6f)
                curveTo(5.47f, 22f, 4.961f, 21.789f, 4.586f, 21.414f)
                curveTo(4.211f, 21.039f, 4f, 20.53f, 4f, 20f)
                verticalLineTo(6f)
                curveTo(4f, 5.47f, 4.211f, 4.961f, 4.586f, 4.586f)
                curveTo(4.961f, 4.211f, 5.47f, 4f, 6f, 4f)
                horizontalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 2f)
                horizontalLineTo(9f)
                curveTo(8.448f, 2f, 8f, 2.448f, 8f, 3f)
                verticalLineTo(5f)
                curveTo(8f, 5.552f, 8.448f, 6f, 9f, 6f)
                horizontalLineTo(15f)
                curveTo(15.552f, 6f, 16f, 5.552f, 16f, 5f)
                verticalLineTo(3f)
                curveTo(16f, 2.448f, 15.552f, 2f, 15f, 2f)
                close()
            }
        }.build()

        return _Clipboard!!
    }

@Suppress("ObjectPropertyName")
private var _Clipboard: ImageVector? = null
