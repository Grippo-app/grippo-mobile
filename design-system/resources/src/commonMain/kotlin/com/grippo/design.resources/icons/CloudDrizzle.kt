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

public val AppIcon.CloudDrizzle: ImageVector
    get() {
        if (_CloudDrizzle != null) {
            return _CloudDrizzle!!
        }
        _CloudDrizzle = ImageVector.Builder(
            name = "CloudDrizzle",
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
                    moveTo(8f, 19f)
                    verticalLineTo(21f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8f, 13f)
                    verticalLineTo(15f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 19f)
                    verticalLineTo(21f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 13f)
                    verticalLineTo(15f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 21f)
                    verticalLineTo(23f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 15f)
                    verticalLineTo(17f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(20f, 16.58f)
                    curveTo(21.051f, 16.12f, 21.912f, 15.311f, 22.438f, 14.292f)
                    curveTo(22.964f, 13.271f, 23.123f, 12.102f, 22.889f, 10.978f)
                    curveTo(22.654f, 9.855f, 22.04f, 8.846f, 21.15f, 8.122f)
                    curveTo(20.26f, 7.397f, 19.148f, 7.001f, 18f, 7f)
                    horizontalLineTo(16.74f)
                    curveTo(16.423f, 5.773f, 15.819f, 4.638f, 14.977f, 3.69f)
                    curveTo(14.136f, 2.742f, 13.081f, 2.007f, 11.9f, 1.547f)
                    curveTo(10.719f, 1.086f, 9.445f, 0.913f, 8.184f, 1.041f)
                    curveTo(6.922f, 1.17f, 5.71f, 1.596f, 4.646f, 2.285f)
                    curveTo(3.582f, 2.974f, 2.697f, 3.906f, 2.063f, 5.005f)
                    curveTo(1.43f, 6.103f, 1.067f, 7.336f, 1.004f, 8.602f)
                    curveTo(0.941f, 9.869f, 1.18f, 11.132f, 1.701f, 12.287f)
                    curveTo(2.222f, 13.443f, 3.01f, 14.459f, 4f, 15.25f)
                }
            }
        }.build()

        return _CloudDrizzle!!
    }

@Suppress("ObjectPropertyName")
private var _CloudDrizzle: ImageVector? = null
