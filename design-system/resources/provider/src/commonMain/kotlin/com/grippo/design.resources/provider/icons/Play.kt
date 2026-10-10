package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Play: ImageVector
    get() {
        if (_Play != null) return _Play!!
        _Play = ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 7f)
                curveTo(8.33f, 6.61f, 7.5f, 7.09f, 7.5f, 7.87f)
                verticalLineTo(16.13f)
                curveTo(7.5f, 16.91f, 8.33f, 17.39f, 9f, 17f)
                lineTo(16.15f, 12.87f)
                curveTo(16.82f, 12.48f, 16.82f, 11.52f, 16.15f, 11.13f)
                lineTo(9f, 7f)
                close()
            }
        }.build()
        return _Play!!
    }

@Suppress("ObjectPropertyName")
private var _Play: ImageVector? = null
