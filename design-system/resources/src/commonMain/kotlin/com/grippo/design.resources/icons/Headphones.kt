package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Headphones: ImageVector
    get() {
        if (_Headphones != null) {
            return _Headphones!!
        }
        _Headphones = ImageVector.Builder(
            name = "Headphones",
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
                moveTo(3f, 18f)
                verticalLineTo(12f)
                curveTo(3f, 9.613f, 3.948f, 7.324f, 5.636f, 5.636f)
                curveTo(7.324f, 3.948f, 9.613f, 3f, 12f, 3f)
                curveTo(14.387f, 3f, 16.676f, 3.948f, 18.364f, 5.636f)
                curveTo(20.052f, 7.324f, 21f, 9.613f, 21f, 12f)
                verticalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 19f)
                curveTo(21f, 19.53f, 20.789f, 20.039f, 20.414f, 20.414f)
                curveTo(20.039f, 20.789f, 19.53f, 21f, 19f, 21f)
                horizontalLineTo(18f)
                curveTo(17.47f, 21f, 16.961f, 20.789f, 16.586f, 20.414f)
                curveTo(16.211f, 20.039f, 16f, 19.53f, 16f, 19f)
                verticalLineTo(16f)
                curveTo(16f, 15.47f, 16.211f, 14.961f, 16.586f, 14.586f)
                curveTo(16.961f, 14.211f, 17.47f, 14f, 18f, 14f)
                horizontalLineTo(21f)
                verticalLineTo(19f)
                close()
                moveTo(3f, 19f)
                curveTo(3f, 19.53f, 3.211f, 20.039f, 3.586f, 20.414f)
                curveTo(3.961f, 20.789f, 4.47f, 21f, 5f, 21f)
                horizontalLineTo(6f)
                curveTo(6.53f, 21f, 7.039f, 20.789f, 7.414f, 20.414f)
                curveTo(7.789f, 20.039f, 8f, 19.53f, 8f, 19f)
                verticalLineTo(16f)
                curveTo(8f, 15.47f, 7.789f, 14.961f, 7.414f, 14.586f)
                curveTo(7.039f, 14.211f, 6.53f, 14f, 6f, 14f)
                horizontalLineTo(3f)
                verticalLineTo(19f)
                close()
            }
        }.build()

        return _Headphones!!
    }

@Suppress("ObjectPropertyName")
private var _Headphones: ImageVector? = null
