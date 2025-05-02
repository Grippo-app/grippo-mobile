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

public val AppIcon.Crop: ImageVector
    get() {
        if (_Crop != null) {
            return _Crop!!
        }
        _Crop = ImageVector.Builder(
            name = "Crop",
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
                    moveTo(6.13f, 1f)
                    lineTo(6f, 16f)
                    curveTo(6f, 16.53f, 6.211f, 17.039f, 6.586f, 17.414f)
                    curveTo(6.961f, 17.789f, 7.47f, 18f, 8f, 18f)
                    horizontalLineTo(23f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(1f, 6.13f)
                    lineTo(16f, 6f)
                    curveTo(16.53f, 6f, 17.039f, 6.211f, 17.414f, 6.586f)
                    curveTo(17.789f, 6.961f, 18f, 7.47f, 18f, 8f)
                    verticalLineTo(23f)
                }
            }
        }.build()

        return _Crop!!
    }

@Suppress("ObjectPropertyName")
private var _Crop: ImageVector? = null
