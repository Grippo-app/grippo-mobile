package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Video: ImageVector
    get() {
        if (_Video != null) {
            return _Video!!
        }
        _Video = ImageVector.Builder(
            name = "Video",
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
                moveTo(23f, 7f)
                lineTo(16f, 12f)
                lineTo(23f, 17f)
                verticalLineTo(7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 5f)
                horizontalLineTo(3f)
                curveTo(1.895f, 5f, 1f, 5.895f, 1f, 7f)
                verticalLineTo(17f)
                curveTo(1f, 18.105f, 1.895f, 19f, 3f, 19f)
                horizontalLineTo(14f)
                curveTo(15.105f, 19f, 16f, 18.105f, 16f, 17f)
                verticalLineTo(7f)
                curveTo(16f, 5.895f, 15.105f, 5f, 14f, 5f)
                close()
            }
        }.build()

        return _Video!!
    }

@Suppress("ObjectPropertyName")
private var _Video: ImageVector? = null
