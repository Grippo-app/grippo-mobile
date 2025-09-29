package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Menu: ImageVector
    get() {
        if (_Menu != null) {
            return _Menu!!
        }
        _Menu = ImageVector.Builder(
            name = "Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = null
            ) {
                val r = 1.5f
                moveTo(12f + r, 6f)
                arcTo(r, r, 0f, true, true, 12f - r, 6f)
                arcTo(r, r, 0f, true, true, 12f + r, 6f)
                close()
                moveTo(12f + r, 12f)
                arcTo(r, r, 0f, true, true, 12f - r, 12f)
                arcTo(r, r, 0f, true, true, 12f + r, 12f)
                close()
                moveTo(12f + r, 18f)
                arcTo(r, r, 0f, true, true, 12f - r, 18f)
                arcTo(r, r, 0f, true, true, 12f + r, 18f)
                close()
            }
        }.build()
        return _Menu!!
    }

@Suppress("ObjectPropertyName")
private var _Menu: ImageVector? = null
