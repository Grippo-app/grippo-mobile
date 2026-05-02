package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Body: ImageVector
    get() {
        if (_Body != null) {
            return _Body!!
        }
        _Body = ImageVector.Builder(
            name = "Body",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head — small soft circle, centered.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 2.5f)
                curveTo(13.381f, 2.5f, 14.5f, 3.619f, 14.5f, 5f)
                curveTo(14.5f, 6.381f, 13.381f, 7.5f, 12f, 7.5f)
                curveTo(10.619f, 7.5f, 9.5f, 6.381f, 9.5f, 5f)
                curveTo(9.5f, 3.619f, 10.619f, 2.5f, 12f, 2.5f)
                close()
            }
            // Slim torso (rounded rectangle, narrow shoulders).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(9.5f, 9f)
                horizontalLineTo(14.5f)
                curveTo(15.328f, 9f, 16f, 9.672f, 16f, 10.5f)
                verticalLineTo(13.5f)
                curveTo(16f, 14.328f, 15.328f, 15f, 14.5f, 15f)
                horizontalLineTo(9.5f)
                curveTo(8.672f, 15f, 8f, 14.328f, 8f, 13.5f)
                verticalLineTo(10.5f)
                curveTo(8f, 9.672f, 8.672f, 9f, 9.5f, 9f)
                close()
            }
            // Left leg.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(10f, 15.5f)
                horizontalLineTo(11.25f)
                curveTo(11.664f, 15.5f, 12f, 15.836f, 12f, 16.25f)
                verticalLineTo(20.75f)
                curveTo(12f, 21.44f, 11.44f, 22f, 10.75f, 22f)
                horizontalLineTo(10.25f)
                curveTo(9.56f, 22f, 9f, 21.44f, 9f, 20.75f)
                verticalLineTo(16.5f)
                curveTo(9f, 15.948f, 9.448f, 15.5f, 10f, 15.5f)
                close()
            }
            // Right leg.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12.75f, 15.5f)
                horizontalLineTo(14f)
                curveTo(14.552f, 15.5f, 15f, 15.948f, 15f, 16.5f)
                verticalLineTo(20.75f)
                curveTo(15f, 21.44f, 14.44f, 22f, 13.75f, 22f)
                horizontalLineTo(13.25f)
                curveTo(12.56f, 22f, 12f, 21.44f, 12f, 20.75f)
                verticalLineTo(16.25f)
                curveTo(12f, 15.836f, 12.336f, 15.5f, 12.75f, 15.5f)
                close()
            }
        }.build()

        return _Body!!
    }

@Suppress("ObjectPropertyName")
private var _Body: ImageVector? = null
