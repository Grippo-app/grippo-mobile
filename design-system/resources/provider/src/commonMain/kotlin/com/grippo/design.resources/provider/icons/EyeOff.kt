package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOff: ImageVector
    get() {
        if (_EyeOff != null) {
            return _EyeOff!!
        }
        _EyeOff = ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(14.657f, 17.581f)
                curveTo(13.905f, 18.166f, 12.975f, 18.512f, 11.967f, 18.512f)
                curveTo(9.469f, 18.512f, 7.444f, 16.39f, 7.444f, 13.773f)
                curveTo(7.444f, 12.716f, 7.774f, 11.739f, 8.332f, 10.951f)
                lineTo(14.657f, 17.581f)
                close()
                moveTo(11.967f, 9.032f)
                curveTo(14.464f, 9.032f, 16.488f, 11.155f, 16.488f, 13.773f)
                curveTo(16.488f, 14.195f, 16.435f, 14.604f, 16.336f, 14.994f)
                lineTo(10.8f, 9.192f)
                curveTo(11.172f, 9.088f, 11.563f, 9.032f, 11.967f, 9.032f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(7.036f, 9.593f)
                curveTo(6.968f, 9.644f, 6.899f, 9.695f, 6.832f, 9.747f)
                curveTo(5.291f, 10.945f, 4.059f, 12.389f, 3.33f, 13.341f)
                curveTo(3.238f, 13.46f, 3.169f, 13.551f, 3.112f, 13.63f)
                curveTo(3.066f, 13.694f, 3.037f, 13.74f, 3.018f, 13.771f)
                curveTo(3.037f, 13.803f, 3.066f, 13.849f, 3.112f, 13.914f)
                curveTo(3.169f, 13.993f, 3.238f, 14.083f, 3.33f, 14.203f)
                curveTo(4.059f, 15.155f, 5.291f, 16.599f, 6.832f, 17.797f)
                curveTo(8.385f, 19.004f, 10.151f, 19.882f, 11.967f, 19.882f)
                curveTo(13.273f, 19.882f, 14.552f, 19.426f, 15.745f, 18.722f)
                lineTo(17.157f, 20.201f)
                curveTo(15.656f, 21.158f, 13.891f, 21.882f, 11.967f, 21.882f)
                curveTo(9.517f, 21.882f, 7.323f, 20.711f, 5.605f, 19.376f)
                curveTo(3.876f, 18.032f, 2.525f, 16.441f, 1.742f, 15.419f)
                curveTo(1.452f, 15.04f, 1f, 14.518f, 1f, 13.771f)
                curveTo(1f, 13.025f, 1.452f, 12.504f, 1.742f, 12.125f)
                curveTo(2.525f, 11.103f, 3.876f, 9.512f, 5.605f, 8.168f)
                curveTo(5.619f, 8.157f, 5.633f, 8.146f, 5.646f, 8.136f)
                lineTo(7.036f, 9.593f)
                close()
                moveTo(11.967f, 5.662f)
                curveTo(14.416f, 5.662f, 16.61f, 6.833f, 18.328f, 8.168f)
                curveTo(20.057f, 9.512f, 21.407f, 11.103f, 22.19f, 12.125f)
                curveTo(22.481f, 12.504f, 22.933f, 13.025f, 22.934f, 13.771f)
                curveTo(22.934f, 14.518f, 22.481f, 15.04f, 22.19f, 15.419f)
                curveTo(21.601f, 16.189f, 20.688f, 17.281f, 19.535f, 18.348f)
                lineTo(18.154f, 16.9f)
                curveTo(19.204f, 15.933f, 20.05f, 14.926f, 20.604f, 14.203f)
                curveTo(20.695f, 14.083f, 20.764f, 13.993f, 20.821f, 13.914f)
                curveTo(20.868f, 13.849f, 20.896f, 13.803f, 20.915f, 13.771f)
                curveTo(20.896f, 13.74f, 20.868f, 13.694f, 20.821f, 13.63f)
                curveTo(20.764f, 13.551f, 20.695f, 13.46f, 20.604f, 13.341f)
                curveTo(19.874f, 12.389f, 18.642f, 10.945f, 17.101f, 9.747f)
                curveTo(15.548f, 8.54f, 13.783f, 7.662f, 11.967f, 7.662f)
                curveTo(11.212f, 7.662f, 10.465f, 7.816f, 9.739f, 8.08f)
                lineTo(8.259f, 6.529f)
                curveTo(9.397f, 6.005f, 10.644f, 5.662f, 11.967f, 5.662f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(3.967f, 2f)
                lineTo(21.967f, 21f)
            }
        }.build()

        return _EyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOff: ImageVector? = null
