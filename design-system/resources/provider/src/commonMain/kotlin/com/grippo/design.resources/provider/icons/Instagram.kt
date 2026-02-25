package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Instagram: ImageVector
    get() {
        if (_Instagram != null) {
            return _Instagram!!
        }
        _Instagram = ImageVector.Builder(
            name = "Instagram",
            defaultWidth = 57.dp,
            defaultHeight = 54.dp,
            viewportWidth = 57f,
            viewportHeight = 54f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(42.095f, 0f)
                horizontalLineTo(14.051f)
                curveTo(6.303f, 0f, 0f, 6.305f, 0f, 14.055f)
                verticalLineTo(39.719f)
                curveTo(0f, 47.469f, 6.303f, 53.774f, 14.051f, 53.774f)
                horizontalLineTo(42.095f)
                curveTo(49.843f, 53.774f, 56.146f, 47.469f, 56.146f, 39.719f)
                verticalLineTo(14.055f)
                curveTo(56.146f, 6.305f, 49.843f, 0f, 42.095f, 0f)
                close()
                moveTo(4.957f, 14.055f)
                curveTo(4.957f, 9.04f, 9.037f, 4.958f, 14.051f, 4.958f)
                horizontalLineTo(42.095f)
                curveTo(47.109f, 4.958f, 51.19f, 9.04f, 51.19f, 14.055f)
                verticalLineTo(39.719f)
                curveTo(51.19f, 44.735f, 47.109f, 48.816f, 42.095f, 48.816f)
                horizontalLineTo(14.051f)
                curveTo(9.037f, 48.816f, 4.957f, 44.735f, 4.957f, 39.719f)
                verticalLineTo(14.055f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(28.073f, 39.958f)
                curveTo(35.278f, 39.958f, 41.143f, 34.094f, 41.143f, 26.884f)
                curveTo(41.143f, 19.674f, 35.281f, 13.81f, 28.073f, 13.81f)
                curveTo(20.865f, 13.81f, 15.003f, 19.674f, 15.003f, 26.884f)
                curveTo(15.003f, 34.094f, 20.865f, 39.958f, 28.073f, 39.958f)
                close()
                moveTo(28.073f, 18.772f)
                curveTo(32.547f, 18.772f, 36.186f, 22.412f, 36.186f, 26.887f)
                curveTo(36.186f, 31.362f, 32.547f, 35.002f, 28.073f, 35.002f)
                curveTo(23.599f, 35.002f, 19.96f, 31.362f, 19.96f, 26.887f)
                curveTo(19.96f, 22.412f, 23.599f, 18.772f, 28.073f, 18.772f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(42.353f, 15.923f)
                curveTo(44.293f, 15.923f, 45.874f, 14.344f, 45.874f, 12.4f)
                curveTo(45.874f, 10.456f, 44.296f, 8.878f, 42.353f, 8.878f)
                curveTo(40.409f, 8.878f, 38.831f, 10.456f, 38.831f, 12.4f)
                curveTo(38.831f, 14.344f, 40.409f, 15.923f, 42.353f, 15.923f)
                close()
            }
        }.build()

        return _Instagram!!
    }

@Suppress("ObjectPropertyName")
private var _Instagram: ImageVector? = null