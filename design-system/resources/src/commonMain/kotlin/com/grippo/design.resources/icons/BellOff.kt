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

public val AppIcon.BellOff: ImageVector
    get() {
        if (_BellOff != null) {
            return _BellOff!!
        }
        _BellOff = ImageVector.Builder(
            name = "BellOff",
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
                    moveTo(13.73f, 21f)
                    curveTo(13.554f, 21.303f, 13.302f, 21.555f, 12.998f, 21.729f)
                    curveTo(12.695f, 21.904f, 12.35f, 21.997f, 12f, 21.997f)
                    curveTo(11.65f, 21.997f, 11.305f, 21.904f, 11.002f, 21.729f)
                    curveTo(10.698f, 21.555f, 10.446f, 21.303f, 10.27f, 21f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(18.63f, 13f)
                    curveTo(18.185f, 11.371f, 17.973f, 9.688f, 18f, 8f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(6.26f, 6.26f)
                    curveTo(6.086f, 6.824f, 5.999f, 7.41f, 6f, 8f)
                    curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                    horizontalLineTo(17f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(18f, 8f)
                    curveTo(18.002f, 6.913f, 17.708f, 5.846f, 17.15f, 4.913f)
                    curveTo(16.593f, 3.98f, 15.792f, 3.216f, 14.834f, 2.703f)
                    curveTo(13.876f, 2.189f, 12.797f, 1.946f, 11.711f, 1.998f)
                    curveTo(10.625f, 2.05f, 9.574f, 2.397f, 8.67f, 3f)
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

        return _BellOff!!
    }

@Suppress("ObjectPropertyName")
private var _BellOff: ImageVector? = null
