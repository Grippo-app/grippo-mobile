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

public val AppIcon.PhoneOff: ImageVector
    get() {
        if (_PhoneOff != null) {
            return _PhoneOff!!
        }
        _PhoneOff = ImageVector.Builder(
            name = "PhoneOff",
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
                    moveTo(5.19f, 12.81f)
                    curveTo(3.506f, 10.213f, 2.454f, 7.257f, 2.12f, 4.18f)
                    curveTo(2.095f, 3.903f, 2.128f, 3.625f, 2.217f, 3.362f)
                    curveTo(2.305f, 3.098f, 2.448f, 2.857f, 2.635f, 2.652f)
                    curveTo(2.822f, 2.447f, 3.05f, 2.283f, 3.304f, 2.171f)
                    curveTo(3.558f, 2.058f, 3.832f, 2f, 4.11f, 2f)
                    horizontalLineTo(7.11f)
                    curveTo(7.595f, 1.995f, 8.066f, 2.167f, 8.434f, 2.484f)
                    curveTo(8.802f, 2.8f, 9.042f, 3.239f, 9.11f, 3.72f)
                    curveTo(9.237f, 4.68f, 9.472f, 5.623f, 9.81f, 6.53f)
                    curveTo(9.945f, 6.888f, 9.974f, 7.277f, 9.894f, 7.651f)
                    curveTo(9.814f, 8.025f, 9.629f, 8.368f, 9.36f, 8.64f)
                    lineTo(8.09f, 9.91f)
                    moveTo(10.68f, 13.31f)
                    curveTo(11.695f, 14.326f, 12.842f, 15.2f, 14.09f, 15.91f)
                    lineTo(15.36f, 14.64f)
                    curveTo(15.632f, 14.371f, 15.975f, 14.186f, 16.349f, 14.106f)
                    curveTo(16.723f, 14.026f, 17.112f, 14.055f, 17.47f, 14.19f)
                    curveTo(18.377f, 14.528f, 19.32f, 14.763f, 20.28f, 14.89f)
                    curveTo(20.761f, 14.958f, 21.2f, 15.198f, 21.517f, 15.566f)
                    curveTo(21.833f, 15.934f, 22.005f, 16.405f, 22f, 16.89f)
                    verticalLineTo(19.89f)
                    curveTo(22.001f, 20.169f, 21.944f, 20.444f, 21.833f, 20.699f)
                    curveTo(21.721f, 20.955f, 21.557f, 21.184f, 21.352f, 21.372f)
                    curveTo(21.147f, 21.56f, 20.905f, 21.704f, 20.641f, 21.793f)
                    curveTo(20.377f, 21.882f, 20.097f, 21.915f, 19.82f, 21.89f)
                    curveTo(16.743f, 21.556f, 13.787f, 20.504f, 11.19f, 18.82f)
                    curveTo(9.985f, 18.055f, 8.869f, 17.16f, 7.86f, 16.15f)
                    lineTo(10.68f, 13.31f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(23f, 1f)
                    lineTo(1f, 23f)
                }
            }
        }.build()

        return _PhoneOff!!
    }

@Suppress("ObjectPropertyName")
private var _PhoneOff: ImageVector? = null
