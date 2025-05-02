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

public val AppIcon.CloudOff: ImageVector
    get() {
        if (_CloudOff != null) {
            return _CloudOff!!
        }
        _CloudOff = ImageVector.Builder(
            name = "CloudOff",
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
                    moveTo(5f, 5f)
                    curveTo(3.438f, 5.864f, 2.21f, 7.225f, 1.511f, 8.867f)
                    curveTo(0.811f, 10.509f, 0.681f, 12.338f, 1.141f, 14.062f)
                    curveTo(1.601f, 15.787f, 2.624f, 17.308f, 4.048f, 18.384f)
                    curveTo(5.473f, 19.46f, 7.215f, 20.029f, 9f, 20f)
                    horizontalLineTo(18f)
                    curveTo(18.58f, 19.999f, 19.155f, 19.898f, 19.7f, 19.7f)
                    moveTo(22.61f, 16.95f)
                    curveTo(22.932f, 16.189f, 23.061f, 15.361f, 22.985f, 14.538f)
                    curveTo(22.908f, 13.715f, 22.629f, 12.925f, 22.173f, 12.236f)
                    curveTo(21.716f, 11.548f, 21.096f, 10.983f, 20.368f, 10.593f)
                    curveTo(19.64f, 10.203f, 18.826f, 9.999f, 18f, 10f)
                    horizontalLineTo(16.74f)
                    curveTo(16.332f, 8.392f, 15.434f, 6.952f, 14.171f, 5.877f)
                    curveTo(12.908f, 4.802f, 11.342f, 4.146f, 9.69f, 4f)
                    lineTo(22.61f, 16.95f)
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

        return _CloudOff!!
    }

@Suppress("ObjectPropertyName")
private var _CloudOff: ImageVector? = null
