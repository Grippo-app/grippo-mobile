package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MediaVideoList: ImageVector
    get() {
        if (_MediaVideoList != null) {
            return _MediaVideoList!!
        }
        _MediaVideoList = ImageVector.Builder(
            name = "MediaVideoList",
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
                moveTo(21f, 7.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(7.6f)
                curveTo(7.269f, 21f, 7f, 20.731f, 7f, 20.4f)
                verticalLineTo(7.6f)
                curveTo(7f, 7.269f, 7.269f, 7f, 7.6f, 7f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 7f, 21f, 7.269f, 21f, 7.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 4f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 4f, 4f, 4.269f, 4f, 4.6f)
                verticalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.909f, 11.545f)
                curveTo(12.509f, 11.305f, 12f, 11.593f, 12f, 12.06f)
                verticalLineTo(15.94f)
                curveTo(12f, 16.407f, 12.509f, 16.695f, 12.909f, 16.455f)
                lineTo(16.142f, 14.514f)
                curveTo(16.531f, 14.281f, 16.531f, 13.719f, 16.142f, 13.486f)
                lineTo(12.909f, 11.545f)
                close()
            }
        }.build()

        return _MediaVideoList!!
    }

@Suppress("ObjectPropertyName")
private var _MediaVideoList: ImageVector? = null
