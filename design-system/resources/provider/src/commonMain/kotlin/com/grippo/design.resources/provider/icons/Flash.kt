package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Flash: ImageVector
    get() {
        if (_Flash != null) {
            return _Flash!!
        }
        _Flash = ImageVector.Builder(
            name = "Flash",
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
                moveTo(8.426f, 13.006f)
                curveTo(8.369f, 13.005f, 8.369f, 13.005f, 8.311f, 13f)
                curveTo(7.678f, 12.93f, 7.216f, 12.302f, 7.279f, 11.598f)
                lineTo(7.89f, 4.153f)
                curveTo(7.948f, 3.499f, 8.444f, 3f, 9.036f, 3f)
                horizontalLineTo(13.499f)
                curveTo(13.627f, 3f, 13.753f, 3.023f, 13.874f, 3.07f)
                curveTo(14.476f, 3.299f, 14.796f, 4.028f, 14.589f, 4.697f)
                lineTo(13.1f, 8.802f)
                lineTo(15.847f, 8.802f)
                curveTo(16.05f, 8.802f, 16.249f, 8.862f, 16.425f, 8.975f)
                curveTo(16.976f, 9.331f, 17.163f, 10.114f, 16.843f, 10.726f)
                curveTo(16.429f, 11.541f, 12.263f, 18.541f, 10.917f, 20.8f)
                curveTo(10.81f, 20.98f, 10.536f, 20.892f, 10.548f, 20.683f)
                lineTo(10.999f, 13f)
                lineTo(8.426f, 13.006f)
                close()
            }
        }.build()

        return _Flash!!
    }

@Suppress("ObjectPropertyName")
private var _Flash: ImageVector? = null
