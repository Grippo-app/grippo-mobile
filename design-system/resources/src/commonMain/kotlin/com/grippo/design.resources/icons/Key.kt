package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Key: ImageVector
    get() {
        if (_Key != null) {
            return _Key!!
        }
        _Key = ImageVector.Builder(
            name = "Key",
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
                moveTo(15.5f, 7.5f)
                lineTo(19f, 4f)
                moveTo(21f, 2f)
                lineTo(19f, 4f)
                lineTo(21f, 2f)
                close()
                moveTo(11.39f, 11.61f)
                curveTo(11.906f, 12.12f, 12.317f, 12.726f, 12.598f, 13.395f)
                curveTo(12.879f, 14.064f, 13.025f, 14.781f, 13.027f, 15.507f)
                curveTo(13.03f, 16.232f, 12.888f, 16.951f, 12.612f, 17.621f)
                curveTo(12.335f, 18.292f, 11.929f, 18.901f, 11.416f, 19.414f)
                curveTo(10.903f, 19.927f, 10.294f, 20.333f, 9.623f, 20.61f)
                curveTo(8.953f, 20.886f, 8.234f, 21.028f, 7.509f, 21.025f)
                curveTo(6.783f, 21.023f, 6.066f, 20.877f, 5.397f, 20.596f)
                curveTo(4.728f, 20.315f, 4.122f, 19.904f, 3.612f, 19.388f)
                curveTo(2.61f, 18.351f, 2.056f, 16.961f, 2.068f, 15.519f)
                curveTo(2.081f, 14.077f, 2.659f, 12.698f, 3.679f, 11.678f)
                curveTo(4.699f, 10.658f, 6.078f, 10.08f, 7.52f, 10.067f)
                curveTo(8.962f, 10.055f, 10.352f, 10.609f, 11.389f, 11.611f)
                lineTo(11.39f, 11.61f)
                close()
                moveTo(11.39f, 11.61f)
                lineTo(15.5f, 7.5f)
                lineTo(11.39f, 11.61f)
                close()
                moveTo(15.5f, 7.5f)
                lineTo(18.5f, 10.5f)
                lineTo(22f, 7f)
                lineTo(19f, 4f)
                lineTo(15.5f, 7.5f)
                close()
            }
        }.build()

        return _Key!!
    }

@Suppress("ObjectPropertyName")
private var _Key: ImageVector? = null
