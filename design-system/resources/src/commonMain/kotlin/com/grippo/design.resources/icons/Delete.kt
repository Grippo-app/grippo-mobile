package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Delete: ImageVector
    get() {
        if (_Delete != null) {
            return _Delete!!
        }
        _Delete = ImageVector.Builder(
            name = "Delete",
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
                moveTo(21f, 4f)
                horizontalLineTo(8f)
                lineTo(1f, 12f)
                lineTo(8f, 20f)
                horizontalLineTo(21f)
                curveTo(21.53f, 20f, 22.039f, 19.789f, 22.414f, 19.414f)
                curveTo(22.789f, 19.039f, 23f, 18.53f, 23f, 18f)
                verticalLineTo(6f)
                curveTo(23f, 5.47f, 22.789f, 4.961f, 22.414f, 4.586f)
                curveTo(22.039f, 4.211f, 21.53f, 4f, 21f, 4f)
                verticalLineTo(4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 9f)
                lineTo(12f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                lineTo(18f, 15f)
            }
        }.build()

        return _Delete!!
    }

@Suppress("ObjectPropertyName")
private var _Delete: ImageVector? = null
