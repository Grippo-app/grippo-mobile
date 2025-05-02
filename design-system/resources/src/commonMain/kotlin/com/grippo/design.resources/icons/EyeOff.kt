package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EyeOff: ImageVector
    get() {
        if (_EyeOff != null) {
            return _EyeOff!!
        }
        _EyeOff = ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(14.12f, 14.12f)
                    curveTo(13.845f, 14.415f, 13.514f, 14.651f, 13.146f, 14.815f)
                    curveTo(12.778f, 14.979f, 12.381f, 15.067f, 11.978f, 15.074f)
                    curveTo(11.575f, 15.082f, 11.175f, 15.007f, 10.802f, 14.856f)
                    curveTo(10.428f, 14.706f, 10.089f, 14.481f, 9.804f, 14.196f)
                    curveTo(9.519f, 13.911f, 9.294f, 13.572f, 9.144f, 13.198f)
                    curveTo(8.993f, 12.825f, 8.919f, 12.425f, 8.926f, 12.022f)
                    curveTo(8.933f, 11.619f, 9.021f, 11.222f, 9.185f, 10.854f)
                    curveTo(9.349f, 10.486f, 9.585f, 10.155f, 9.88f, 9.88f)
                    moveTo(17.94f, 17.94f)
                    curveTo(16.231f, 19.243f, 14.149f, 19.965f, 12f, 20f)
                    curveTo(5f, 20f, 1f, 12f, 1f, 12f)
                    curveTo(2.244f, 9.682f, 3.969f, 7.657f, 6.06f, 6.06f)
                    lineTo(17.94f, 17.94f)
                    close()
                    moveTo(9.9f, 4.24f)
                    curveTo(10.588f, 4.079f, 11.293f, 3.998f, 12f, 4f)
                    curveTo(19f, 4f, 23f, 12f, 23f, 12f)
                    curveTo(22.393f, 13.136f, 21.669f, 14.205f, 20.84f, 15.19f)
                    lineTo(9.9f, 4.24f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(1f, 1f)
                    lineTo(23f, 23f)
                }
            }
        }.build()

        return _EyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOff: ImageVector? = null
