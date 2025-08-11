package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BeachBag: ImageVector
    get() {
        if (_BeachBag != null) {
            return _BeachBag!!
        }
        _BeachBag = ImageVector.Builder(
            name = "BeachBag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(20.615f, 17f)
                horizontalLineTo(3.385f)
                moveTo(2.769f, 13f)
                lineTo(2.137f, 9.713f)
                curveTo(2.066f, 9.343f, 2.35f, 9f, 2.726f, 9f)
                horizontalLineTo(21.274f)
                curveTo(21.65f, 9f, 21.934f, 9.343f, 21.863f, 9.713f)
                lineTo(21.231f, 13f)
                horizontalLineTo(2.769f)
                close()
                moveTo(2.769f, 13f)
                horizontalLineTo(21.231f)
                horizontalLineTo(2.769f)
                close()
                moveTo(2.769f, 13f)
                lineTo(3.385f, 17f)
                lineTo(2.769f, 13f)
                close()
                moveTo(21.231f, 13f)
                lineTo(20.615f, 17f)
                lineTo(21.231f, 13f)
                close()
                moveTo(20.615f, 17f)
                lineTo(20.078f, 20.491f)
                curveTo(20.033f, 20.784f, 19.781f, 21f, 19.485f, 21f)
                horizontalLineTo(4.515f)
                curveTo(4.219f, 21f, 3.967f, 20.784f, 3.922f, 20.491f)
                lineTo(3.385f, 17f)
                horizontalLineTo(20.615f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(8f, 9f)
                verticalLineTo(5f)
                curveTo(8f, 3.895f, 8.895f, 3f, 10f, 3f)
                horizontalLineTo(14f)
                curveTo(15.105f, 3f, 16f, 3.895f, 16f, 5f)
                verticalLineTo(9f)
            }
        }.build()

        return _BeachBag!!
    }

@Suppress("ObjectPropertyName")
private var _BeachBag: ImageVector? = null
