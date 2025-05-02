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

public val AppIcon.DownloadCloud: ImageVector
    get() {
        if (_DownloadCloud != null) {
            return _DownloadCloud!!
        }
        _DownloadCloud = ImageVector.Builder(
            name = "DownloadCloud",
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
                    moveTo(8f, 17f)
                    lineTo(12f, 21f)
                    lineTo(16f, 17f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 12f)
                    verticalLineTo(21f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(20.88f, 18.09f)
                    curveTo(21.749f, 17.479f, 22.401f, 16.606f, 22.741f, 15.599f)
                    curveTo(23.081f, 14.592f, 23.091f, 13.503f, 22.77f, 12.49f)
                    curveTo(22.449f, 11.477f, 21.814f, 10.592f, 20.956f, 9.965f)
                    curveTo(20.098f, 9.337f, 19.063f, 8.999f, 18f, 9f)
                    horizontalLineTo(16.74f)
                    curveTo(16.439f, 7.828f, 15.877f, 6.739f, 15.094f, 5.816f)
                    curveTo(14.312f, 4.893f, 13.33f, 4.159f, 12.223f, 3.67f)
                    curveTo(11.116f, 3.181f, 9.913f, 2.95f, 8.704f, 2.994f)
                    curveTo(7.494f, 3.037f, 6.311f, 3.354f, 5.242f, 3.921f)
                    curveTo(4.173f, 4.489f, 3.246f, 5.291f, 2.532f, 6.268f)
                    curveTo(1.818f, 7.245f, 1.335f, 8.371f, 1.119f, 9.562f)
                    curveTo(0.903f, 10.752f, 0.961f, 11.977f, 1.286f, 13.142f)
                    curveTo(1.612f, 14.307f, 2.198f, 15.384f, 3f, 16.29f)
                }
            }
        }.build()

        return _DownloadCloud!!
    }

@Suppress("ObjectPropertyName")
private var _DownloadCloud: ImageVector? = null
