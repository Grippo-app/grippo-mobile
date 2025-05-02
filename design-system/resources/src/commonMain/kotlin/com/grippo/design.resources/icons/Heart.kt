package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Heart: ImageVector
    get() {
        if (_Heart != null) {
            return _Heart!!
        }
        _Heart = ImageVector.Builder(
            name = "Heart",
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
                moveTo(20.84f, 4.61f)
                curveTo(20.329f, 4.099f, 19.723f, 3.694f, 19.055f, 3.417f)
                curveTo(18.388f, 3.141f, 17.673f, 2.998f, 16.95f, 2.998f)
                curveTo(16.228f, 2.998f, 15.512f, 3.141f, 14.845f, 3.417f)
                curveTo(14.177f, 3.694f, 13.571f, 4.099f, 13.06f, 4.61f)
                lineTo(12f, 5.67f)
                lineTo(10.94f, 4.61f)
                curveTo(9.908f, 3.578f, 8.509f, 2.999f, 7.05f, 2.999f)
                curveTo(5.591f, 2.999f, 4.192f, 3.578f, 3.16f, 4.61f)
                curveTo(2.128f, 5.642f, 1.549f, 7.041f, 1.549f, 8.5f)
                curveTo(1.549f, 9.959f, 2.128f, 11.358f, 3.16f, 12.39f)
                lineTo(4.22f, 13.45f)
                lineTo(12f, 21.23f)
                lineTo(19.78f, 13.45f)
                lineTo(20.84f, 12.39f)
                curveTo(21.351f, 11.879f, 21.756f, 11.273f, 22.033f, 10.605f)
                curveTo(22.309f, 9.938f, 22.452f, 9.222f, 22.452f, 8.5f)
                curveTo(22.452f, 7.778f, 22.309f, 7.062f, 22.033f, 6.395f)
                curveTo(21.756f, 5.727f, 21.351f, 5.121f, 20.84f, 4.61f)
                verticalLineTo(4.61f)
                close()
            }
        }.build()

        return _Heart!!
    }

@Suppress("ObjectPropertyName")
private var _Heart: ImageVector? = null
