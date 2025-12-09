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
                moveTo(14.211f, 15.332f)
                curveTo(13.577f, 15.753f, 12.818f, 16f, 12f, 16f)
                curveTo(9.791f, 16f, 8f, 14.209f, 8f, 12f)
                curveTo(8f, 11.182f, 8.246f, 10.422f, 8.667f, 9.788f)
                lineTo(14.211f, 15.332f)
                close()
                moveTo(12f, 8f)
                curveTo(14.209f, 8f, 16f, 9.791f, 16f, 12f)
                curveTo(16f, 12.274f, 15.972f, 12.541f, 15.92f, 12.799f)
                lineTo(11.2f, 8.079f)
                curveTo(11.459f, 8.027f, 11.726f, 8f, 12f, 8f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(7.574f, 8.695f)
                curveTo(6.294f, 9.633f, 5.25f, 10.762f, 4.584f, 11.57f)
                curveTo(4.485f, 11.691f, 4.41f, 11.782f, 4.348f, 11.861f)
                curveTo(4.299f, 11.924f, 4.27f, 11.969f, 4.249f, 12f)
                curveTo(4.27f, 12.031f, 4.299f, 12.076f, 4.348f, 12.139f)
                curveTo(4.41f, 12.218f, 4.485f, 12.309f, 4.584f, 12.43f)
                curveTo(5.264f, 13.255f, 6.34f, 14.418f, 7.659f, 15.367f)
                curveTo(8.986f, 16.322f, 10.474f, 17f, 12f, 17f)
                curveTo(13.048f, 17f, 14.077f, 16.679f, 15.049f, 16.17f)
                lineTo(16.518f, 17.639f)
                curveTo(15.205f, 18.417f, 13.67f, 19f, 12f, 19f)
                curveTo(9.89f, 19f, 7.994f, 18.073f, 6.491f, 16.991f)
                curveTo(4.981f, 15.904f, 3.78f, 14.599f, 3.04f, 13.701f)
                curveTo(2.744f, 13.342f, 2.291f, 12.855f, 2.236f, 12.144f)
                lineTo(2.23f, 12f)
                lineTo(2.236f, 11.856f)
                curveTo(2.291f, 11.145f, 2.744f, 10.658f, 3.04f, 10.299f)
                curveTo(3.723f, 9.47f, 4.797f, 8.296f, 6.145f, 7.266f)
                lineTo(7.574f, 8.695f)
                close()
                moveTo(12f, 5f)
                curveTo(14.11f, 5f, 16.006f, 5.927f, 17.509f, 7.009f)
                curveTo(19.02f, 8.096f, 20.22f, 9.401f, 20.96f, 10.299f)
                curveTo(21.276f, 10.682f, 21.771f, 11.211f, 21.771f, 12f)
                curveTo(21.771f, 12.789f, 21.276f, 13.318f, 20.96f, 13.701f)
                curveTo(20.473f, 14.292f, 19.785f, 15.056f, 18.943f, 15.821f)
                lineTo(17.527f, 14.405f)
                curveTo(18.307f, 13.706f, 18.954f, 12.991f, 19.416f, 12.43f)
                curveTo(19.516f, 12.309f, 19.591f, 12.218f, 19.653f, 12.139f)
                curveTo(19.701f, 12.076f, 19.73f, 12.031f, 19.75f, 12f)
                curveTo(19.73f, 11.969f, 19.701f, 11.924f, 19.653f, 11.861f)
                curveTo(19.591f, 11.782f, 19.516f, 11.691f, 19.416f, 11.57f)
                curveTo(18.736f, 10.745f, 17.66f, 9.582f, 16.341f, 8.633f)
                curveTo(15.014f, 7.678f, 13.526f, 7f, 12f, 7f)
                curveTo(11.449f, 7f, 10.904f, 7.089f, 10.368f, 7.247f)
                lineTo(8.807f, 5.686f)
                curveTo(9.791f, 5.269f, 10.865f, 5f, 12f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(5f, 2f)
                lineTo(21f, 18f)
            }
        }.build()

        return _EyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOff: ImageVector? = null
