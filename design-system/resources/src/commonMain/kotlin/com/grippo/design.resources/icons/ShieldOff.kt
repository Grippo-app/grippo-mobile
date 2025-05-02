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

public val AppIcon.ShieldOff: ImageVector
    get() {
        if (_ShieldOff != null) {
            return _ShieldOff!!
        }
        _ShieldOff = ImageVector.Builder(
            name = "ShieldOff",
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
                    moveTo(19.69f, 14f)
                    curveTo(19.891f, 13.352f, 19.996f, 12.678f, 20f, 12f)
                    verticalLineTo(5f)
                    lineTo(12f, 2f)
                    lineTo(8.84f, 3.18f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(4.73f, 4.73f)
                    lineTo(4f, 5f)
                    verticalLineTo(12f)
                    curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                    curveTo(14.117f, 20.883f, 16.02f, 19.4f, 17.62f, 17.62f)
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

        return _ShieldOff!!
    }

@Suppress("ObjectPropertyName")
private var _ShieldOff: ImageVector? = null
