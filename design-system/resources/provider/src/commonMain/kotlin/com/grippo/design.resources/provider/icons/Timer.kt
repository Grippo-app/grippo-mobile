package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Timer: ImageVector
    get() {
        if (_Timer != null) {
            return _Timer!!
        }
        _Timer = ImageVector.Builder(
            name = "Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top knob (rounded pill)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(10f, 1f)
                horizontalLineTo(14f)
                curveTo(14.552f, 1f, 15f, 1.448f, 15f, 2f)
                curveTo(15f, 2.552f, 14.552f, 3f, 14f, 3f)
                horizontalLineTo(10f)
                curveTo(9.448f, 3f, 9f, 2.552f, 9f, 2f)
                curveTo(9f, 1.448f, 9.448f, 1f, 10f, 1f)
                close()
            }
            // Clock face filled with hand cut-outs (rounded)
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer circle
                moveTo(12f, 4f)
                curveTo(16.971f, 4f, 21f, 8.029f, 21f, 13f)
                curveTo(21f, 17.971f, 16.971f, 22f, 12f, 22f)
                curveTo(7.029f, 22f, 3f, 17.971f, 3f, 13f)
                curveTo(3f, 8.029f, 7.029f, 4f, 12f, 4f)
                close()
                // Vertical hand cut-out (rounded pill)
                moveTo(12f, 7f)
                curveTo(11.448f, 7f, 11f, 7.448f, 11f, 8f)
                verticalLineTo(13f)
                curveTo(11f, 13.552f, 11.448f, 14f, 12f, 14f)
                curveTo(12.552f, 14f, 13f, 13.552f, 13f, 13f)
                verticalLineTo(8f)
                curveTo(13f, 7.448f, 12.552f, 7f, 12f, 7f)
                close()
                // Horizontal hand cut-out (rounded pill)
                moveTo(12f, 12f)
                curveTo(11.448f, 12f, 11f, 12.448f, 11f, 13f)
                curveTo(11f, 13.552f, 11.448f, 14f, 12f, 14f)
                horizontalLineTo(16f)
                curveTo(16.552f, 14f, 17f, 13.552f, 17f, 13f)
                curveTo(17f, 12.448f, 16.552f, 12f, 16f, 12f)
                close()
            }
        }.build()

        return _Timer!!
    }

@Suppress("ObjectPropertyName")
private var _Timer: ImageVector? = null
