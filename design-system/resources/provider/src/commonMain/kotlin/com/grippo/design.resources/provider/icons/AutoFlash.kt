package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AutoFlash: ImageVector
    get() {
        if (_AutoFlash != null) {
            return _AutoFlash!!
        }
        _AutoFlash = ImageVector.Builder(
            name = "AutoFlash",
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
                moveTo(3.426f, 13.006f)
                curveTo(3.369f, 13.005f, 3.369f, 13.005f, 3.311f, 13f)
                curveTo(2.678f, 12.93f, 2.216f, 12.302f, 2.279f, 11.598f)
                lineTo(2.89f, 4.153f)
                curveTo(2.948f, 3.499f, 3.444f, 3f, 4.036f, 3f)
                horizontalLineTo(8.499f)
                curveTo(8.627f, 3f, 8.753f, 3.023f, 8.874f, 3.07f)
                curveTo(9.476f, 3.299f, 9.796f, 4.028f, 9.589f, 4.697f)
                lineTo(8.1f, 8.802f)
                lineTo(10.847f, 8.802f)
                curveTo(11.05f, 8.802f, 11.249f, 8.862f, 11.425f, 8.975f)
                curveTo(11.976f, 9.331f, 12.163f, 10.114f, 11.843f, 10.726f)
                curveTo(11.429f, 11.541f, 7.263f, 18.541f, 5.917f, 20.8f)
                curveTo(5.81f, 20.98f, 5.536f, 20.892f, 5.548f, 20.683f)
                lineTo(5.999f, 13f)
                lineTo(3.426f, 13.006f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.308f, 8f)
                horizontalLineTo(16.692f)
                moveTo(16f, 9.5f)
                lineTo(16.692f, 8f)
                lineTo(16f, 9.5f)
                close()
                moveTo(22f, 9.5f)
                lineTo(21.308f, 8f)
                lineTo(22f, 9.5f)
                close()
                moveTo(21.308f, 8f)
                lineTo(19f, 3f)
                lineTo(16.692f, 8f)
                horizontalLineTo(21.308f)
                close()
            }
        }.build()

        return _AutoFlash!!
    }

@Suppress("ObjectPropertyName")
private var _AutoFlash: ImageVector? = null
