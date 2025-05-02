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

public val AppIcon.CameraOff: ImageVector
    get() {
        if (_CameraOff != null) {
            return _CameraOff!!
        }
        _CameraOff = ImageVector.Builder(
            name = "CameraOff",
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
                    moveTo(15.28f, 15.28f)
                    curveTo(14.948f, 15.765f, 14.513f, 16.171f, 14.007f, 16.469f)
                    curveTo(13.5f, 16.767f, 12.934f, 16.95f, 12.349f, 17.004f)
                    curveTo(11.764f, 17.058f, 11.174f, 16.983f, 10.621f, 16.784f)
                    curveTo(10.068f, 16.584f, 9.566f, 16.265f, 9.15f, 15.85f)
                    curveTo(8.735f, 15.434f, 8.416f, 14.932f, 8.216f, 14.379f)
                    curveTo(8.017f, 13.826f, 7.942f, 13.236f, 7.996f, 12.651f)
                    curveTo(8.05f, 12.066f, 8.233f, 11.5f, 8.531f, 10.993f)
                    curveTo(8.829f, 10.487f, 9.235f, 10.052f, 9.72f, 9.72f)
                    moveTo(21f, 21f)
                    horizontalLineTo(3f)
                    curveTo(2.47f, 21f, 1.961f, 20.789f, 1.586f, 20.414f)
                    curveTo(1.211f, 20.039f, 1f, 19.53f, 1f, 19f)
                    verticalLineTo(8f)
                    curveTo(1f, 7.47f, 1.211f, 6.961f, 1.586f, 6.586f)
                    curveTo(1.961f, 6.211f, 2.47f, 6f, 3f, 6f)
                    horizontalLineTo(6f)
                    lineTo(21f, 21f)
                    close()
                    moveTo(9f, 3f)
                    horizontalLineTo(15f)
                    lineTo(17f, 6f)
                    horizontalLineTo(21f)
                    curveTo(21.53f, 6f, 22.039f, 6.211f, 22.414f, 6.586f)
                    curveTo(22.789f, 6.961f, 23f, 7.47f, 23f, 8f)
                    verticalLineTo(17.34f)
                    lineTo(9f, 3f)
                    close()
                }
            }
        }.build()

        return _CameraOff!!
    }

@Suppress("ObjectPropertyName")
private var _CameraOff: ImageVector? = null
