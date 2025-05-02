package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ZoomOut: ImageVector
    get() {
        if (_ZoomOut != null) {
            return _ZoomOut!!
        }
        _ZoomOut = ImageVector.Builder(
            name = "ZoomOut",
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
                moveTo(11f, 19f)
                curveTo(15.418f, 19f, 19f, 15.418f, 19f, 11f)
                curveTo(19f, 6.582f, 15.418f, 3f, 11f, 3f)
                curveTo(6.582f, 3f, 3f, 6.582f, 3f, 11f)
                curveTo(3f, 15.418f, 6.582f, 19f, 11f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 11f)
                horizontalLineTo(14f)
            }
        }.build()

        return _ZoomOut!!
    }

@Suppress("ObjectPropertyName")
private var _ZoomOut: ImageVector? = null
