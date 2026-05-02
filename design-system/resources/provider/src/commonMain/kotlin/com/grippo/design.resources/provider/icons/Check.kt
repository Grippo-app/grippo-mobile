package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Check: ImageVector
    get() {
        if (_Check != null) {
            return _Check!!
        }
        _Check = ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Chunky filled checkmark (read as "check-fat")
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(20.7f, 6.3f)
                curveTo(21.1f, 6.7f, 21.1f, 7.3f, 20.7f, 7.7f)
                lineTo(10.5f, 17.9f)
                curveTo(10.1f, 18.3f, 9.5f, 18.3f, 9.1f, 17.9f)
                lineTo(3.3f, 12.1f)
                curveTo(2.9f, 11.7f, 2.9f, 11.1f, 3.3f, 10.7f)
                lineTo(4.7f, 9.3f)
                curveTo(5.1f, 8.9f, 5.7f, 8.9f, 6.1f, 9.3f)
                lineTo(9.8f, 13.0f)
                lineTo(17.9f, 4.9f)
                curveTo(18.3f, 4.5f, 18.9f, 4.5f, 19.3f, 4.9f)
                close()
            }
        }.build()

        return _Check!!
    }

@Suppress("ObjectPropertyName")
private var _Check: ImageVector? = null
