package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FlagEn: ImageVector
    get() {
        if (_FlagEn != null) return _FlagEn!!
        _FlagEn = ImageVector.Builder(
            name = "FlagEn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Circle background (white)
            path(fill = SolidColor(Color(0xFFFFFFFF))) {
                moveTo(12f, 2f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, 20f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, -20f)
                close()
            }
            // Horizontal cross bar (red), kept inside circle by margins
            path(fill = SolidColor(Color(0xFFCE1126))) {
                moveTo(4f, 10.5f)
                lineTo(20f, 10.5f)
                lineTo(20f, 13.5f)
                lineTo(4f, 13.5f)
                close()
            }
            // Vertical cross bar (red), kept inside circle by margins
            path(fill = SolidColor(Color(0xFFCE1126))) {
                moveTo(10.5f, 4f)
                lineTo(13.5f, 4f)
                lineTo(13.5f, 20f)
                lineTo(10.5f, 20f)
                close()
            }
        }.build()
        return _FlagEn!!
    }

@Suppress("ObjectPropertyName")
private var _FlagEn: ImageVector? = null