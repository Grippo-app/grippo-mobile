package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BeachBagBig: ImageVector
    get() {
        if (_BeachBagBig != null) {
            return _BeachBagBig!!
        }
        _BeachBagBig = ImageVector.Builder(
            name = "BeachBagBig",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(5f, 8f)
                verticalLineTo(6f)
                curveTo(5f, 4.895f, 5.895f, 4f, 7f, 4f)
                horizontalLineTo(17f)
                curveTo(18.105f, 4f, 19f, 4.895f, 19f, 6f)
                verticalLineTo(8f)
                moveTo(2.769f, 12f)
                lineTo(2.137f, 8.713f)
                curveTo(2.066f, 8.343f, 2.35f, 8f, 2.726f, 8f)
                horizontalLineTo(21.274f)
                curveTo(21.65f, 8f, 21.934f, 8.343f, 21.863f, 8.713f)
                lineTo(21.231f, 12f)
                horizontalLineTo(2.769f)
                close()
                moveTo(2.769f, 12f)
                horizontalLineTo(21.231f)
                horizontalLineTo(2.769f)
                close()
                moveTo(2.769f, 12f)
                lineTo(3.385f, 16f)
                lineTo(2.769f, 12f)
                close()
                moveTo(21.231f, 12f)
                lineTo(20.615f, 16f)
                lineTo(21.231f, 12f)
                close()
                moveTo(20.615f, 16f)
                lineTo(20.078f, 19.491f)
                curveTo(20.033f, 19.784f, 19.781f, 20f, 19.485f, 20f)
                horizontalLineTo(4.515f)
                curveTo(4.219f, 20f, 3.967f, 19.784f, 3.922f, 19.491f)
                lineTo(3.385f, 16f)
                horizontalLineTo(20.615f)
                close()
                moveTo(20.615f, 16f)
                horizontalLineTo(3.385f)
                horizontalLineTo(20.615f)
                close()
            }
        }.build()

        return _BeachBagBig!!
    }

@Suppress("ObjectPropertyName")
private var _BeachBagBig: ImageVector? = null
