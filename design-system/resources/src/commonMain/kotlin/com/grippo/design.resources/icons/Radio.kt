package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Radio: ImageVector
    get() {
        if (_Radio != null) {
            return _Radio!!
        }
        _Radio = ImageVector.Builder(
            name = "Radio",
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
                moveTo(12f, 14f)
                curveTo(13.105f, 14f, 14f, 13.105f, 14f, 12f)
                curveTo(14f, 10.895f, 13.105f, 10f, 12f, 10f)
                curveTo(10.895f, 10f, 10f, 10.895f, 10f, 12f)
                curveTo(10f, 13.105f, 10.895f, 14f, 12f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.93f, 19.07f)
                curveTo(3.055f, 17.195f, 2.002f, 14.652f, 2.002f, 12f)
                curveTo(2.002f, 9.348f, 3.055f, 6.805f, 4.93f, 4.93f)
                moveTo(16.24f, 7.76f)
                curveTo(16.798f, 8.317f, 17.24f, 8.979f, 17.542f, 9.707f)
                curveTo(17.844f, 10.436f, 18f, 11.217f, 18f, 12.005f)
                curveTo(18f, 12.793f, 17.844f, 13.574f, 17.542f, 14.303f)
                curveTo(17.24f, 15.031f, 16.798f, 15.693f, 16.24f, 16.25f)
                verticalLineTo(7.76f)
                close()
                moveTo(7.76f, 16.24f)
                curveTo(7.202f, 15.683f, 6.76f, 15.021f, 6.458f, 14.293f)
                curveTo(6.156f, 13.564f, 6f, 12.783f, 6f, 11.995f)
                curveTo(6f, 11.207f, 6.156f, 10.426f, 6.458f, 9.697f)
                curveTo(6.76f, 8.969f, 7.202f, 8.307f, 7.76f, 7.75f)
                verticalLineTo(16.24f)
                close()
                moveTo(19.07f, 4.93f)
                curveTo(20.945f, 6.805f, 21.998f, 9.348f, 21.998f, 12f)
                curveTo(21.998f, 14.652f, 20.945f, 17.195f, 19.07f, 19.07f)
                verticalLineTo(4.93f)
                close()
            }
        }.build()

        return _Radio!!
    }

@Suppress("ObjectPropertyName")
private var _Radio: ImageVector? = null
