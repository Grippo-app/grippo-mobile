package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Tiktok: ImageVector
    get() {
        if (_Tiktok != null) {
            return _Tiktok!!
        }
        _Tiktok = ImageVector.Builder(
            name = "Tiktok",
            defaultWidth = 44.dp,
            defaultHeight = 50.dp,
            viewportWidth = 44f,
            viewportHeight = 50f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(43.016f, 12.025f)
                verticalLineTo(20.585f)
                curveTo(41.521f, 20.439f, 39.577f, 20.099f, 37.428f, 19.312f)
                curveTo(34.621f, 18.283f, 32.531f, 16.875f, 31.163f, 15.786f)
                verticalLineTo(33.087f)
                lineTo(31.128f, 33.033f)
                curveTo(31.15f, 33.376f, 31.163f, 33.726f, 31.163f, 34.078f)
                curveTo(31.163f, 42.67f, 24.174f, 49.664f, 15.581f, 49.664f)
                curveTo(6.989f, 49.664f, 0f, 42.67f, 0f, 34.078f)
                curveTo(0f, 25.486f, 6.989f, 18.489f, 15.581f, 18.489f)
                curveTo(16.423f, 18.489f, 17.248f, 18.556f, 18.055f, 18.686f)
                verticalLineTo(27.122f)
                curveTo(17.28f, 26.846f, 16.448f, 26.697f, 15.581f, 26.697f)
                curveTo(11.514f, 26.697f, 8.202f, 30.006f, 8.202f, 34.078f)
                curveTo(8.202f, 38.15f, 11.514f, 41.46f, 15.581f, 41.46f)
                curveTo(19.649f, 41.46f, 22.961f, 38.147f, 22.961f, 34.078f)
                curveTo(22.961f, 33.926f, 22.958f, 33.773f, 22.948f, 33.621f)
                verticalLineTo(0f)
                horizontalLineTo(31.499f)
                curveTo(31.531f, 0.724f, 31.56f, 1.455f, 31.591f, 2.179f)
                curveTo(31.649f, 3.605f, 32.157f, 4.974f, 33.042f, 6.095f)
                curveTo(34.081f, 7.413f, 35.614f, 8.944f, 37.767f, 10.167f)
                curveTo(39.784f, 11.307f, 41.676f, 11.8f, 43.016f, 12.032f)
                verticalLineTo(12.025f)
                close()
            }
        }.build()

        return _Tiktok!!
    }

@Suppress("ObjectPropertyName")
private var _Tiktok: ImageVector? = null
