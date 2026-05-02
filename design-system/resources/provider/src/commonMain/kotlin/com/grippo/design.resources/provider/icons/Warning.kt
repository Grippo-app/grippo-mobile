package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Warning: ImageVector
    get() {
        if (_Warning != null) {
            return _Warning!!
        }
        _Warning = ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Filled circle with exclamation cut-out
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer circle
                moveTo(12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                close()
                // Exclamation stem cut-out
                moveTo(11f, 6.5f)
                horizontalLineTo(13f)
                verticalLineTo(14f)
                horizontalLineTo(11f)
                close()
                // Dot cut-out
                moveTo(12f, 15.5f)
                curveTo(11.31f, 15.5f, 10.75f, 16.06f, 10.75f, 16.75f)
                curveTo(10.75f, 17.44f, 11.31f, 18f, 12f, 18f)
                curveTo(12.69f, 18f, 13.25f, 17.44f, 13.25f, 16.75f)
                curveTo(13.25f, 16.06f, 12.69f, 15.5f, 12f, 15.5f)
                close()
            }
        }.build()

        return _Warning!!
    }

@Suppress("ObjectPropertyName")
private var _Warning: ImageVector? = null
