package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Intensity: ImageVector
    get() {
        if (_Intensity != null) {
            return _Intensity!!
        }
        _Intensity = ImageVector.Builder(
            name = "Intensity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(15.5f, 9.097f)
                curveTo(15.5f, 7.985f, 16.974f, 7.69f, 17.365f, 8.73f)
                curveTo(18.311f, 11.246f, 19f, 13.497f, 19f, 14.714f)
                curveTo(19f, 18.592f, 15.866f, 21.736f, 12f, 21.736f)
                curveTo(8.134f, 21.736f, 5f, 18.592f, 5f, 14.714f)
                curveTo(5f, 13.406f, 5.796f, 10.906f, 6.85f, 8.165f)
                curveTo(8.216f, 4.615f, 8.899f, 2.839f, 9.743f, 2.744f)
                curveTo(10.013f, 2.713f, 10.307f, 2.768f, 10.548f, 2.894f)
                curveTo(11.3f, 3.288f, 11.3f, 5.224f, 11.3f, 9.097f)
                curveTo(11.3f, 10.26f, 12.24f, 11.203f, 13.4f, 11.203f)
                curveTo(14.56f, 11.203f, 15.5f, 10.26f, 15.5f, 9.097f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(10.5f, 21f)
                lineTo(10.183f, 20.259f)
                curveTo(9.459f, 18.57f, 9.758f, 16.616f, 10.954f, 15.221f)
                curveTo(11.504f, 14.579f, 12.496f, 14.579f, 13.046f, 15.221f)
                curveTo(14.242f, 16.616f, 14.541f, 18.57f, 13.817f, 20.259f)
                lineTo(13.5f, 21f)
            }
        }.build()

        return _Intensity!!
    }

@Suppress("ObjectPropertyName")
private var _Intensity: ImageVector? = null

