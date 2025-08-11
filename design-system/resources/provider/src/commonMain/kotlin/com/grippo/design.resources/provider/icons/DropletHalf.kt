package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DropletHalf: ImageVector
    get() {
        if (_DropletHalf != null) {
            return _DropletHalf!!
        }
        _DropletHalf = ImageVector.Builder(
            name = "DropletHalf",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(5.333f, 16f)
                lineTo(17.759f, 10.171f)
                moveTo(5.333f, 16f)
                curveTo(5.117f, 15.307f, 5f, 14.567f, 5f, 13.8f)
                curveTo(5f, 9.824f, 12f, 3f, 12f, 3f)
                curveTo(12f, 3f, 15.748f, 6.653f, 17.759f, 10.171f)
                lineTo(5.333f, 16f)
                close()
                moveTo(5.333f, 16f)
                curveTo(6.237f, 18.9f, 8.88f, 21f, 12f, 21f)
                curveTo(15.866f, 21f, 19f, 17.777f, 19f, 13.8f)
                curveTo(19f, 12.733f, 18.496f, 11.461f, 17.759f, 10.171f)
                lineTo(5.333f, 16f)
                close()
            }
        }.build()

        return _DropletHalf!!
    }

@Suppress("ObjectPropertyName")
private var _DropletHalf: ImageVector? = null
