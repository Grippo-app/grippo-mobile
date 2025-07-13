package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MediaVideoFolder: ImageVector
    get() {
        if (_MediaVideoFolder != null) {
            return _MediaVideoFolder!!
        }
        _MediaVideoFolder = ImageVector.Builder(
            name = "MediaVideoFolder",
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
                moveTo(22f, 12.6f)
                verticalLineTo(20.4f)
                curveTo(22f, 20.731f, 21.731f, 21f, 21.4f, 21f)
                horizontalLineTo(13.6f)
                curveTo(13.269f, 21f, 13f, 20.731f, 13f, 20.4f)
                verticalLineTo(12.6f)
                curveTo(13f, 12.269f, 13.269f, 12f, 13.6f, 12f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 12f, 22f, 12.269f, 22f, 12.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.918f, 14.574f)
                curveTo(16.518f, 14.324f, 16f, 14.611f, 16f, 15.083f)
                verticalLineTo(17.918f)
                curveTo(16f, 18.389f, 16.518f, 18.676f, 16.918f, 18.426f)
                lineTo(19.186f, 17.009f)
                curveTo(19.562f, 16.774f, 19.562f, 16.226f, 19.186f, 15.991f)
                lineTo(16.918f, 14.574f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 10f)
                horizontalLineTo(10f)
                moveTo(2f, 10f)
                verticalLineTo(3.6f)
                curveTo(2f, 3.269f, 2.269f, 3f, 2.6f, 3f)
                horizontalLineTo(8.778f)
                curveTo(8.921f, 3f, 9.06f, 3.051f, 9.169f, 3.144f)
                lineTo(12.332f, 5.856f)
                curveTo(12.44f, 5.949f, 12.579f, 6f, 12.722f, 6f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 6f, 22f, 6.269f, 22f, 6.6f)
                verticalLineTo(9f)
                lineTo(2f, 10f)
                close()
                moveTo(2f, 10f)
                verticalLineTo(18.4f)
                curveTo(2f, 18.731f, 2.269f, 19f, 2.6f, 19f)
                horizontalLineTo(10f)
                lineTo(2f, 10f)
                close()
            }
        }.build()

        return _MediaVideoFolder!!
    }

@Suppress("ObjectPropertyName")
private var _MediaVideoFolder: ImageVector? = null
