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

public val AppIcon.UploadCloud: ImageVector
    get() {
        if (_UploadCloud != null) {
            return _UploadCloud!!
        }
        _UploadCloud = ImageVector.Builder(
            name = "UploadCloud",
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
                    moveTo(16f, 16f)
                    lineTo(12f, 12f)
                    lineTo(8f, 16f)
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
                    moveTo(20.39f, 18.39f)
                    curveTo(21.365f, 17.858f, 22.136f, 17.017f, 22.58f, 15.999f)
                    curveTo(23.024f, 14.98f, 23.116f, 13.843f, 22.842f, 12.767f)
                    curveTo(22.568f, 11.69f, 21.943f, 10.736f, 21.067f, 10.053f)
                    curveTo(20.19f, 9.371f, 19.111f, 9.001f, 18f, 9f)
                    horizontalLineTo(16.74f)
                    curveTo(16.437f, 7.829f, 15.873f, 6.742f, 15.09f, 5.821f)
                    curveTo(14.307f, 4.9f, 13.325f, 4.168f, 12.218f, 3.681f)
                    curveTo(11.111f, 3.193f, 9.909f, 2.963f, 8.7f, 3.008f)
                    curveTo(7.492f, 3.052f, 6.309f, 3.37f, 5.241f, 3.938f)
                    curveTo(4.173f, 4.505f, 3.248f, 5.307f, 2.535f, 6.284f)
                    curveTo(1.821f, 7.26f, 1.339f, 8.386f, 1.123f, 9.575f)
                    curveTo(0.907f, 10.765f, 0.964f, 11.988f, 1.289f, 13.153f)
                    curveTo(1.614f, 14.318f, 2.199f, 15.394f, 3f, 16.3f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 16f)
                    lineTo(12f, 12f)
                    lineTo(8f, 16f)
                }
            }
        }.build()

        return _UploadCloud!!
    }

@Suppress("ObjectPropertyName")
private var _UploadCloud: ImageVector? = null
