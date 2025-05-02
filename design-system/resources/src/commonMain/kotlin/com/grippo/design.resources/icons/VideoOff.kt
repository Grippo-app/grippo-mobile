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

public val AppIcon.VideoOff: ImageVector
    get() {
        if (_VideoOff != null) {
            return _VideoOff!!
        }
        _VideoOff = ImageVector.Builder(
            name = "VideoOff",
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
                    moveTo(10.66f, 5f)
                    horizontalLineTo(14f)
                    curveTo(14.53f, 5f, 15.039f, 5.211f, 15.414f, 5.586f)
                    curveTo(15.789f, 5.961f, 16f, 6.47f, 16f, 7f)
                    verticalLineTo(10.34f)
                    lineTo(17f, 11.34f)
                    lineTo(23f, 7f)
                    verticalLineTo(17f)
                    moveTo(16f, 16f)
                    verticalLineTo(17f)
                    curveTo(16f, 17.53f, 15.789f, 18.039f, 15.414f, 18.414f)
                    curveTo(15.039f, 18.789f, 14.53f, 19f, 14f, 19f)
                    horizontalLineTo(3f)
                    curveTo(2.47f, 19f, 1.961f, 18.789f, 1.586f, 18.414f)
                    curveTo(1.211f, 18.039f, 1f, 17.53f, 1f, 17f)
                    verticalLineTo(7f)
                    curveTo(1f, 6.47f, 1.211f, 5.961f, 1.586f, 5.586f)
                    curveTo(1.961f, 5.211f, 2.47f, 5f, 3f, 5f)
                    horizontalLineTo(5f)
                    lineTo(16f, 16f)
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

        return _VideoOff!!
    }

@Suppress("ObjectPropertyName")
private var _VideoOff: ImageVector? = null
