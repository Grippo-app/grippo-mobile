package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Logout: ImageVector
    get() {
        if (_Logout != null) {
            return _Logout!!
        }
        _Logout = ImageVector.Builder(
            name = "Logout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Door — narrow rectangle (x = 3..12) with all corners rounded.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(5f, 3f)
                horizontalLineTo(10f)
                curveTo(11.105f, 3f, 12f, 3.895f, 12f, 5f)
                verticalLineTo(7f)
                curveTo(12f, 7.552f, 11.552f, 8f, 11f, 8f)
                curveTo(10.448f, 8f, 10f, 7.552f, 10f, 7f)
                verticalLineTo(5.5f)
                curveTo(10f, 5.224f, 9.776f, 5f, 9.5f, 5f)
                horizontalLineTo(5.5f)
                curveTo(5.224f, 5f, 5f, 5.224f, 5f, 5.5f)
                verticalLineTo(18.5f)
                curveTo(5f, 18.776f, 5.224f, 19f, 5.5f, 19f)
                horizontalLineTo(9.5f)
                curveTo(9.776f, 19f, 10f, 18.776f, 10f, 18.5f)
                verticalLineTo(17f)
                curveTo(10f, 16.448f, 10.448f, 16f, 11f, 16f)
                curveTo(11.552f, 16f, 12f, 16.448f, 12f, 17f)
                verticalLineTo(19f)
                curveTo(12f, 20.105f, 11.105f, 21f, 10f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                close()
            }
            // Arrow — sleek pointed shape: short shaft + sharp triangular head.
            // Shaft pill from x = 12.5..17 at y = 11..13. Head triangle from x = 17..21
            // with tip at (21, 12). Tail rounded cap touches door right edge at x = 12.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(13f, 11f)
                horizontalLineTo(17f)
                verticalLineTo(8f)
                curveTo(17f, 7.11f, 18.07f, 6.67f, 18.7f, 7.3f)
                lineTo(20.7f, 9.3f)
                curveTo(22.183f, 10.783f, 22.183f, 13.217f, 20.7f, 14.7f)
                lineTo(18.7f, 16.7f)
                curveTo(18.07f, 17.33f, 17f, 16.89f, 17f, 16f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                curveTo(12.448f, 13f, 12f, 12.552f, 12f, 12f)
                curveTo(12f, 11.448f, 12.448f, 11f, 13f, 11f)
                close()
            }
        }.build()

        return _Logout!!
    }

@Suppress("ObjectPropertyName")
private var _Logout: ImageVector? = null
