package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Edit: ImageVector
    get() {
        if (_Edit != null) {
            return _Edit!!
        }
        _Edit = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 4f)
                horizontalLineTo(4f)
                curveTo(3.47f, 4f, 2.961f, 4.211f, 2.586f, 4.586f)
                curveTo(2.211f, 4.961f, 2f, 5.47f, 2f, 6f)
                verticalLineTo(20f)
                curveTo(2f, 20.53f, 2.211f, 21.039f, 2.586f, 21.414f)
                curveTo(2.961f, 21.789f, 3.47f, 22f, 4f, 22f)
                horizontalLineTo(18f)
                curveTo(18.53f, 22f, 19.039f, 21.789f, 19.414f, 21.414f)
                curveTo(19.789f, 21.039f, 20f, 20.53f, 20f, 20f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 2.5f)
                curveTo(18.898f, 2.102f, 19.437f, 1.879f, 20f, 1.879f)
                curveTo(20.563f, 1.879f, 21.102f, 2.102f, 21.5f, 2.5f)
                curveTo(21.898f, 2.898f, 22.121f, 3.437f, 22.121f, 4f)
                curveTo(22.121f, 4.563f, 21.898f, 5.102f, 21.5f, 5.5f)
                lineTo(12f, 15f)
                lineTo(8f, 16f)
                lineTo(9f, 12f)
                lineTo(18.5f, 2.5f)
                close()
            }
        }.build()

        return _Edit!!
    }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null
