package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Maximize: ImageVector
    get() {
        if (_Maximize != null) {
            return _Maximize!!
        }
        _Maximize = ImageVector.Builder(
            name = "Maximize",
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
                moveTo(3f, 16f)
                verticalLineTo(19f)
                curveTo(3f, 19.53f, 3.211f, 20.039f, 3.586f, 20.414f)
                curveTo(3.961f, 20.789f, 4.47f, 21f, 5f, 21f)
                horizontalLineTo(8f)
                moveTo(8f, 3f)
                horizontalLineTo(5f)
                curveTo(4.47f, 3f, 3.961f, 3.211f, 3.586f, 3.586f)
                curveTo(3.211f, 3.961f, 3f, 4.47f, 3f, 5f)
                verticalLineTo(8f)
                lineTo(8f, 3f)
                close()
                moveTo(21f, 8f)
                verticalLineTo(5f)
                curveTo(21f, 4.47f, 20.789f, 3.961f, 20.414f, 3.586f)
                curveTo(20.039f, 3.211f, 19.53f, 3f, 19f, 3f)
                horizontalLineTo(16f)
                lineTo(21f, 8f)
                close()
                moveTo(16f, 21f)
                horizontalLineTo(19f)
                curveTo(19.53f, 21f, 20.039f, 20.789f, 20.414f, 20.414f)
                curveTo(20.789f, 20.039f, 21f, 19.53f, 21f, 19f)
                verticalLineTo(16f)
                lineTo(16f, 21f)
                close()
            }
        }.build()

        return _Maximize!!
    }

@Suppress("ObjectPropertyName")
private var _Maximize: ImageVector? = null
