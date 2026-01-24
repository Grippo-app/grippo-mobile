package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Apple: ImageVector
    get() {
        if (_Apple != null) {
            return _Apple!!
        }
        _Apple = ImageVector.Builder(
            name = "AppleIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12f, 5.2f)
                curveTo(9.4f, 5.2f, 7.2f, 7.3f, 7.2f, 10.1f)
                curveTo(7.2f, 13.5f, 9.6f, 18.2f, 12f, 18.2f)
                curveTo(14.4f, 18.2f, 16.8f, 13.5f, 16.8f, 10.1f)
                curveTo(16.8f, 7.3f, 14.6f, 5.2f, 12f, 5.2f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(14.6f, 3.2f)
                curveTo(14.2f, 2.1f, 15.1f, 1f, 16.2f, 1f)
                curveTo(16.5f, 2.2f, 15.6f, 3.3f, 14.6f, 3.2f)
                close()
            }
        }.build()

        return _Apple!!
    }

@Suppress("ObjectPropertyName")
private var _Apple: ImageVector? = null
