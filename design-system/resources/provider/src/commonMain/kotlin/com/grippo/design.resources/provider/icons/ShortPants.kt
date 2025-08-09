package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ShortPants: ImageVector
    get() {
        if (_ShortPants != null) {
            return _ShortPants!!
        }
        _ShortPants = ImageVector.Builder(
            name = "ShortPants",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3.06f, 5.655f)
                curveTo(3.028f, 5.303f, 3.305f, 5f, 3.658f, 5f)
                horizontalLineTo(20.342f)
                curveTo(20.695f, 5f, 20.972f, 5.303f, 20.94f, 5.655f)
                lineTo(19.764f, 18.455f)
                curveTo(19.736f, 18.764f, 19.477f, 19f, 19.167f, 19f)
                horizontalLineTo(15.015f)
                curveTo(14.752f, 19f, 14.519f, 18.828f, 14.441f, 18.576f)
                lineTo(12.574f, 12.474f)
                curveTo(12.401f, 11.908f, 11.599f, 11.908f, 11.426f, 12.474f)
                lineTo(9.559f, 18.576f)
                curveTo(9.481f, 18.828f, 9.249f, 19f, 8.985f, 19f)
                horizontalLineTo(4.833f)
                curveTo(4.523f, 19f, 4.264f, 18.764f, 4.236f, 18.455f)
                lineTo(3.643f, 12f)
                lineTo(3.06f, 5.655f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(4f, 9.5f)
                horizontalLineTo(5.5f)
                curveTo(6.605f, 9.5f, 7.5f, 8.605f, 7.5f, 7.5f)
                verticalLineTo(5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(20.5f, 9.5f)
                horizontalLineTo(18.5f)
                curveTo(17.395f, 9.5f, 16.5f, 8.605f, 16.5f, 7.5f)
                verticalLineTo(5f)
            }
        }.build()

        return _ShortPants!!
    }

@Suppress("ObjectPropertyName")
private var _ShortPants: ImageVector? = null
