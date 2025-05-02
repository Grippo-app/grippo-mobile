package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Minimize: ImageVector
    get() {
        if (_Minimize != null) {
            return _Minimize!!
        }
        _Minimize = ImageVector.Builder(
            name = "Minimize",
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
                horizontalLineTo(6f)
                curveTo(6.53f, 16f, 7.039f, 16.211f, 7.414f, 16.586f)
                curveTo(7.789f, 16.961f, 8f, 17.47f, 8f, 18f)
                verticalLineTo(21f)
                moveTo(8f, 3f)
                verticalLineTo(6f)
                curveTo(8f, 6.53f, 7.789f, 7.039f, 7.414f, 7.414f)
                curveTo(7.039f, 7.789f, 6.53f, 8f, 6f, 8f)
                horizontalLineTo(3f)
                lineTo(8f, 3f)
                close()
                moveTo(21f, 8f)
                horizontalLineTo(18f)
                curveTo(17.47f, 8f, 16.961f, 7.789f, 16.586f, 7.414f)
                curveTo(16.211f, 7.039f, 16f, 6.53f, 16f, 6f)
                verticalLineTo(3f)
                lineTo(21f, 8f)
                close()
                moveTo(16f, 21f)
                verticalLineTo(18f)
                curveTo(16f, 17.47f, 16.211f, 16.961f, 16.586f, 16.586f)
                curveTo(16.961f, 16.211f, 17.47f, 16f, 18f, 16f)
                horizontalLineTo(21f)
                lineTo(16f, 21f)
                close()
            }
        }.build()

        return _Minimize!!
    }

@Suppress("ObjectPropertyName")
private var _Minimize: ImageVector? = null
