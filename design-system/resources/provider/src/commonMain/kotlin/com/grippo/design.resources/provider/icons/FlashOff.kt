package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FlashOff: ImageVector
    get() {
        if (_FlashOff != null) {
            return _FlashOff!!
        }
        _FlashOff = ImageVector.Builder(
            name = "FlashOff",
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
                moveTo(7.585f, 7.876f)
                lineTo(7.28f, 11.598f)
                curveTo(7.217f, 12.302f, 7.679f, 12.93f, 8.312f, 13f)
                curveTo(8.37f, 13.005f, 8.37f, 13.005f, 8.427f, 13.006f)
                lineTo(11f, 13f)
                lineTo(10.549f, 20.683f)
                curveTo(10.537f, 20.892f, 10.813f, 20.977f, 10.92f, 20.797f)
                curveTo(11.597f, 19.662f, 12.982f, 17.333f, 14.247f, 15.193f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 3f)
                horizontalLineTo(13.5f)
                curveTo(13.627f, 3f, 13.754f, 3.023f, 13.875f, 3.07f)
                curveTo(14.477f, 3.299f, 14.797f, 4.028f, 14.59f, 4.697f)
                lineTo(13.101f, 8.802f)
                lineTo(15.847f, 8.802f)
                curveTo(16.051f, 8.802f, 16.25f, 8.862f, 16.426f, 8.975f)
                curveTo(16.976f, 9.331f, 17.164f, 10.114f, 16.844f, 10.726f)
                curveTo(16.775f, 10.862f, 16.602f, 11.169f, 16.355f, 11.598f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }.build()

        return _FlashOff!!
    }

@Suppress("ObjectPropertyName")
private var _FlashOff: ImageVector? = null
