package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Bug: ImageVector
    get() {
        if (_Bug != null) {
            return _Bug!!
        }
        _Bug = ImageVector.Builder(
            name = "Bug",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Body — soft capsule (head + thorax)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 6f)
                curveTo(9.239f, 6f, 7f, 8.239f, 7f, 11f)
                verticalLineTo(16f)
                curveTo(7f, 18.761f, 9.239f, 21f, 12f, 21f)
                curveTo(14.761f, 21f, 17f, 18.761f, 17f, 16f)
                verticalLineTo(11f)
                curveTo(17f, 8.239f, 14.761f, 6f, 12f, 6f)
                close()
            }
            // Antennae left — soft rounded line
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(7.2f, 2.4f)
                curveTo(7.6f, 2f, 8.4f, 2f, 8.8f, 2.4f)
                lineTo(11.4f, 6.2f)
                curveTo(11.7f, 6.7f, 11.5f, 7.3f, 11f, 7.6f)
                curveTo(10.5f, 7.9f, 9.9f, 7.7f, 9.6f, 7.2f)
                lineTo(7f, 3.5f)
                curveTo(6.7f, 3.1f, 6.8f, 2.7f, 7.2f, 2.4f)
                close()
            }
            // Antennae right
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(16.8f, 2.4f)
                curveTo(17.2f, 2.7f, 17.3f, 3.1f, 17f, 3.5f)
                lineTo(14.4f, 7.2f)
                curveTo(14.1f, 7.7f, 13.5f, 7.9f, 13f, 7.6f)
                curveTo(12.5f, 7.3f, 12.3f, 6.7f, 12.6f, 6.2f)
                lineTo(15.2f, 2.4f)
                curveTo(15.6f, 2f, 16.4f, 2f, 16.8f, 2.4f)
                close()
            }
            // Top-left leg (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(3f, 10f)
                curveTo(3f, 9.448f, 3.448f, 9f, 4f, 9f)
                horizontalLineTo(6f)
                curveTo(6.552f, 9f, 7f, 9.448f, 7f, 10f)
                curveTo(7f, 10.552f, 6.552f, 11f, 6f, 11f)
                horizontalLineTo(5f)
                verticalLineTo(13f)
                curveTo(5f, 13.552f, 4.552f, 14f, 4f, 14f)
                curveTo(3.448f, 14f, 3f, 13.552f, 3f, 13f)
                close()
            }
            // Bottom-left leg
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(3f, 18f)
                curveTo(3f, 17.448f, 3.448f, 17f, 4f, 17f)
                curveTo(4.552f, 17f, 5f, 17.448f, 5f, 18f)
                verticalLineTo(20f)
                horizontalLineTo(6f)
                curveTo(6.552f, 20f, 7f, 20.448f, 7f, 21f)
                curveTo(7f, 21.552f, 6.552f, 22f, 6f, 22f)
                horizontalLineTo(4f)
                curveTo(3.448f, 22f, 3f, 21.552f, 3f, 21f)
                close()
            }
            // Top-right leg
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(17f, 10f)
                curveTo(17f, 9.448f, 17.448f, 9f, 18f, 9f)
                horizontalLineTo(20f)
                curveTo(20.552f, 9f, 21f, 9.448f, 21f, 10f)
                verticalLineTo(13f)
                curveTo(21f, 13.552f, 20.552f, 14f, 20f, 14f)
                curveTo(19.448f, 14f, 19f, 13.552f, 19f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(18f)
                curveTo(17.448f, 11f, 17f, 10.552f, 17f, 10f)
                close()
            }
            // Bottom-right leg
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(17f, 21f)
                curveTo(17f, 20.448f, 17.448f, 20f, 18f, 20f)
                horizontalLineTo(19f)
                verticalLineTo(18f)
                curveTo(19f, 17.448f, 19.448f, 17f, 20f, 17f)
                curveTo(20.552f, 17f, 21f, 17.448f, 21f, 18f)
                verticalLineTo(21f)
                curveTo(21f, 21.552f, 20.552f, 22f, 20f, 22f)
                horizontalLineTo(18f)
                curveTo(17.448f, 22f, 17f, 21.552f, 17f, 21f)
                close()
            }
        }.build()

        return _Bug!!
    }

@Suppress("ObjectPropertyName")
private var _Bug: ImageVector? = null
