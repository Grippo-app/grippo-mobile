package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ZoomIn: ImageVector
    get() {
        if (_ZoomIn != null) {
            return _ZoomIn!!
        }
        _ZoomIn = ImageVector.Builder(
            name = "ZoomIn",
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
                moveTo(11f, 11f)
                verticalLineTo(13f)
                moveTo(9f, 11f)
                horizontalLineTo(11f)
                horizontalLineTo(9f)
                close()
                moveTo(13f, 11f)
                horizontalLineTo(11f)
                horizontalLineTo(13f)
                close()
                moveTo(11f, 11f)
                verticalLineTo(9f)
                verticalLineTo(11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 16f)
                lineTo(20f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 11f)
                curveTo(4f, 14.866f, 7.134f, 18f, 11f, 18f)
                curveTo(12.936f, 18f, 14.689f, 17.214f, 15.956f, 15.943f)
                curveTo(17.219f, 14.677f, 18f, 12.93f, 18f, 11f)
                curveTo(18f, 7.134f, 14.866f, 4f, 11f, 4f)
                curveTo(7.134f, 4f, 4f, 7.134f, 4f, 11f)
                close()
            }
        }.build()

        return _ZoomIn!!
    }

@Suppress("ObjectPropertyName")
private var _ZoomIn: ImageVector? = null
