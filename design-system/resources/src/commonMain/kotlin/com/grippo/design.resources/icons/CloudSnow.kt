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

public val AppIcon.CloudSnow: ImageVector
    get() {
        if (_CloudSnow != null) {
            return _CloudSnow!!
        }
        _CloudSnow = ImageVector.Builder(
            name = "CloudSnow",
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
                    moveTo(20f, 17.58f)
                    curveTo(21.051f, 17.12f, 21.912f, 16.312f, 22.438f, 15.292f)
                    curveTo(22.964f, 14.271f, 23.123f, 13.102f, 22.889f, 11.978f)
                    curveTo(22.654f, 10.855f, 22.04f, 9.846f, 21.15f, 9.122f)
                    curveTo(20.26f, 8.397f, 19.148f, 8.001f, 18f, 8f)
                    horizontalLineTo(16.74f)
                    curveTo(16.423f, 6.773f, 15.819f, 5.638f, 14.977f, 4.69f)
                    curveTo(14.136f, 3.742f, 13.081f, 3.007f, 11.9f, 2.547f)
                    curveTo(10.719f, 2.086f, 9.445f, 1.913f, 8.184f, 2.041f)
                    curveTo(6.922f, 2.17f, 5.71f, 2.596f, 4.646f, 3.285f)
                    curveTo(3.582f, 3.974f, 2.696f, 4.906f, 2.063f, 6.005f)
                    curveTo(1.43f, 7.103f, 1.067f, 8.336f, 1.004f, 9.602f)
                    curveTo(0.941f, 10.868f, 1.18f, 12.132f, 1.701f, 13.287f)
                    curveTo(2.221f, 14.443f, 3.01f, 15.459f, 4f, 16.25f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8f, 16f)
                    horizontalLineTo(8.01f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8f, 20f)
                    horizontalLineTo(8.01f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 18f)
                    horizontalLineTo(12.01f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 22f)
                    horizontalLineTo(12.01f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 16f)
                    horizontalLineTo(16.01f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 20f)
                    horizontalLineTo(16.01f)
                }
            }
        }.build()

        return _CloudSnow!!
    }

@Suppress("ObjectPropertyName")
private var _CloudSnow: ImageVector? = null
