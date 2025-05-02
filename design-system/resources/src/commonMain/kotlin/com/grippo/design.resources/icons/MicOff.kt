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

public val AppIcon.MicOff: ImageVector
    get() {
        if (_MicOff != null) {
            return _MicOff!!
        }
        _MicOff = ImageVector.Builder(
            name = "MicOff",
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
                    moveTo(1f, 1f)
                    lineTo(23f, 23f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(15f, 9.34f)
                    verticalLineTo(4f)
                    curveTo(15.001f, 3.256f, 14.725f, 2.538f, 14.226f, 1.986f)
                    curveTo(13.728f, 1.434f, 13.042f, 1.087f, 12.302f, 1.012f)
                    curveTo(11.561f, 0.937f, 10.82f, 1.14f, 10.221f, 1.582f)
                    curveTo(9.622f, 2.023f, 9.208f, 2.671f, 9.06f, 3.4f)
                    moveTo(9f, 9f)
                    verticalLineTo(12f)
                    curveTo(9.001f, 12.593f, 9.177f, 13.172f, 9.506f, 13.665f)
                    curveTo(9.836f, 14.158f, 10.304f, 14.542f, 10.852f, 14.769f)
                    curveTo(11.4f, 14.996f, 12.003f, 15.055f, 12.585f, 14.94f)
                    curveTo(13.166f, 14.824f, 13.7f, 14.539f, 14.12f, 14.12f)
                    lineTo(9f, 9f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19f, 10f)
                    verticalLineTo(12f)
                    curveTo(19f, 12.412f, 18.963f, 12.824f, 18.89f, 13.23f)
                    moveTo(17f, 16.95f)
                    curveTo(16.024f, 17.946f, 14.772f, 18.628f, 13.406f, 18.909f)
                    curveTo(12.039f, 19.189f, 10.62f, 19.054f, 9.33f, 18.522f)
                    curveTo(8.041f, 17.99f, 6.94f, 17.085f, 6.168f, 15.923f)
                    curveTo(5.397f, 14.761f, 4.99f, 13.395f, 5f, 12f)
                    verticalLineTo(10f)
                    lineTo(17f, 16.95f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 19f)
                    verticalLineTo(23f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8f, 23f)
                    horizontalLineTo(16f)
                }
            }
        }.build()

        return _MicOff!!
    }

@Suppress("ObjectPropertyName")
private var _MicOff: ImageVector? = null
