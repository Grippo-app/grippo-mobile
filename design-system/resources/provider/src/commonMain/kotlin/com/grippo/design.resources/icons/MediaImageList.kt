package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MediaImageList: ImageVector
    get() {
        if (_MediaImageList != null) {
            return _MediaImageList!!
        }
        _MediaImageList = ImageVector.Builder(
            name = "MediaImageList",
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
                moveTo(7f, 16.8f)
                lineTo(12.444f, 15f)
                lineTo(21f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 13f)
                curveTo(15.672f, 13f, 15f, 12.328f, 15f, 11.5f)
                curveTo(15f, 10.672f, 15.672f, 10f, 16.5f, 10f)
                curveTo(17.328f, 10f, 18f, 10.672f, 18f, 11.5f)
                curveTo(18f, 12.328f, 17.328f, 13f, 16.5f, 13f)
                close()
            }
        }.build()

        return _MediaImageList!!
    }

@Suppress("ObjectPropertyName")
private var _MediaImageList: ImageVector? = null
