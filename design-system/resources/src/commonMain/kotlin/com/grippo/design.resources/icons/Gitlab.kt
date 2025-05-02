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

public val AppIcon.Gitlab: ImageVector
    get() {
        if (_Gitlab != null) {
            return _Gitlab!!
        }
        _Gitlab = ImageVector.Builder(
            name = "Gitlab",
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
                    moveTo(22.65f, 14.39f)
                    lineTo(12f, 22.13f)
                    lineTo(1.35f, 14.39f)
                    curveTo(1.207f, 14.285f, 1.101f, 14.138f, 1.047f, 13.969f)
                    curveTo(0.994f, 13.8f, 0.994f, 13.618f, 1.05f, 13.45f)
                    lineTo(2.27f, 9.67f)
                    lineTo(4.71f, 2.16f)
                    curveTo(4.734f, 2.099f, 4.771f, 2.044f, 4.82f, 2f)
                    curveTo(4.899f, 1.928f, 5.003f, 1.888f, 5.11f, 1.888f)
                    curveTo(5.217f, 1.888f, 5.321f, 1.928f, 5.4f, 2f)
                    curveTo(5.451f, 2.05f, 5.489f, 2.112f, 5.51f, 2.18f)
                    lineTo(7.95f, 9.67f)
                    horizontalLineTo(16.05f)
                    lineTo(18.49f, 2.16f)
                    curveTo(18.514f, 2.099f, 18.551f, 2.044f, 18.6f, 2f)
                    curveTo(18.679f, 1.928f, 18.783f, 1.888f, 18.89f, 1.888f)
                    curveTo(18.997f, 1.888f, 19.101f, 1.928f, 19.18f, 2f)
                    curveTo(19.231f, 2.05f, 19.269f, 2.112f, 19.29f, 2.18f)
                    lineTo(21.73f, 9.69f)
                    lineTo(23f, 13.45f)
                    curveTo(23.051f, 13.623f, 23.044f, 13.809f, 22.981f, 13.978f)
                    curveTo(22.918f, 14.147f, 22.802f, 14.292f, 22.65f, 14.39f)
                    verticalLineTo(14.39f)
                    close()
                }
            }
        }.build()

        return _Gitlab!!
    }

@Suppress("ObjectPropertyName")
private var _Gitlab: ImageVector? = null
