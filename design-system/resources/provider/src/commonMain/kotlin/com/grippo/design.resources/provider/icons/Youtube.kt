package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Youtube: ImageVector
    get() {
        if (_Youtube != null) {
            return _Youtube!!
        }
        _Youtube = ImageVector.Builder(
            name = "Youtube",
            defaultWidth = 58.dp,
            defaultHeight = 41.dp,
            viewportWidth = 58f,
            viewportHeight = 41f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(57.405f, 10.825f)
                curveTo(57.049f, 7.391f, 56.284f, 3.596f, 53.467f, 1.601f)
                curveTo(51.286f, 0.054f, 48.419f, -0.003f, 45.742f, 0f)
                curveTo(40.083f, 0f, 34.422f, 0.01f, 28.763f, 0.013f)
                curveTo(23.321f, 0.019f, 17.878f, 0.022f, 12.436f, 0.029f)
                curveTo(10.162f, 0.029f, 7.952f, -0.146f, 5.841f, 0.839f)
                curveTo(4.027f, 1.684f, 2.608f, 3.291f, 1.754f, 5.079f)
                curveTo(0.569f, 7.566f, 0.322f, 10.383f, 0.179f, 13.134f)
                curveTo(-0.085f, 18.143f, -0.056f, 23.165f, 0.258f, 28.17f)
                curveTo(0.49f, 31.823f, 1.078f, 35.86f, 3.9f, 38.188f)
                curveTo(6.403f, 40.25f, 9.934f, 40.351f, 13.179f, 40.355f)
                curveTo(23.479f, 40.364f, 33.784f, 40.374f, 44.087f, 40.38f)
                curveTo(45.408f, 40.383f, 46.786f, 40.358f, 48.133f, 40.212f)
                curveTo(50.781f, 39.926f, 53.305f, 39.167f, 55.007f, 37.204f)
                curveTo(56.725f, 35.225f, 57.167f, 32.471f, 57.427f, 29.863f)
                curveTo(58.062f, 23.536f, 58.056f, 17.149f, 57.405f, 10.825f)
                close()
                moveTo(22.806f, 29.06f)
                verticalLineTo(11.32f)
                lineTo(38.162f, 20.188f)
                lineTo(22.806f, 29.06f)
                close()
            }
        }.build()

        return _Youtube!!
    }

@Suppress("ObjectPropertyName")
private var _Youtube: ImageVector? = null