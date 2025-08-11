package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Bag: ImageVector
    get() {
        if (_Bag != null) {
            return _Bag!!
        }
        _Bag = ImageVector.Builder(
            name = "Bag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(4.508f, 20f)
                horizontalLineTo(19.492f)
                curveTo(19.785f, 20f, 20.035f, 19.788f, 20.084f, 19.499f)
                lineTo(21.884f, 8.699f)
                curveTo(21.944f, 8.333f, 21.663f, 8f, 21.292f, 8f)
                horizontalLineTo(2.708f)
                curveTo(2.338f, 8f, 2.056f, 8.333f, 2.116f, 8.699f)
                lineTo(3.916f, 19.499f)
                curveTo(3.965f, 19.788f, 4.215f, 20f, 4.508f, 20f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7f, 8f)
                verticalLineTo(6f)
                curveTo(7f, 4.895f, 7.895f, 4f, 9f, 4f)
                horizontalLineTo(15f)
                curveTo(16.105f, 4f, 17f, 4.895f, 17f, 6f)
                verticalLineTo(8f)
            }
        }.build()

        return _Bag!!
    }

@Suppress("ObjectPropertyName")
private var _Bag: ImageVector? = null
