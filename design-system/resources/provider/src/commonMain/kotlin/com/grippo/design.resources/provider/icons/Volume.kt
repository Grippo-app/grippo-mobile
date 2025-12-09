package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Volume: ImageVector
    get() {
        if (_Volume != null) {
            return _Volume!!
        }
        _Volume = ImageVector.Builder(
            name = "Volume",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 14f)
                curveTo(5f, 10.134f, 8.134f, 7f, 12f, 7f)
                curveTo(15.866f, 7f, 19f, 10.134f, 19f, 14f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(16f, 8f)
                curveTo(16.717f, 5.848f, 17.076f, 4.773f, 16.618f, 3.99f)
                curveTo(16.554f, 3.88f, 16.48f, 3.777f, 16.396f, 3.682f)
                curveTo(15.798f, 3f, 14.664f, 3f, 12.396f, 3f)
                horizontalLineTo(11.604f)
                curveTo(9.336f, 3f, 8.202f, 3f, 7.604f, 3.682f)
                curveTo(7.521f, 3.777f, 7.446f, 3.88f, 7.382f, 3.99f)
                curveTo(6.924f, 4.773f, 7.283f, 5.848f, 8f, 8f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 14f)
                curveTo(5f, 17.866f, 8.134f, 21f, 12f, 21f)
                curveTo(15.866f, 21f, 19f, 17.866f, 19f, 14f)
            }
        }.build()

        return _Volume!!
    }

@Suppress("ObjectPropertyName")
private var _Volume: ImageVector? = null
