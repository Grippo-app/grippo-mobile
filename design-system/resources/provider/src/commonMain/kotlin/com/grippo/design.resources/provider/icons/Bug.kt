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
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(7f, 14.333f)
                curveTo(7f, 13.087f, 7f, 12.464f, 7.268f, 12f)
                curveTo(7.443f, 11.696f, 7.696f, 11.443f, 8f, 11.268f)
                curveTo(8.464f, 11f, 9.087f, 11f, 10.333f, 11f)
                horizontalLineTo(13.667f)
                curveTo(14.913f, 11f, 15.536f, 11f, 16f, 11.268f)
                curveTo(16.304f, 11.443f, 16.556f, 11.696f, 16.732f, 12f)
                curveTo(17f, 12.464f, 17f, 13.087f, 17f, 14.333f)
                verticalLineTo(16f)
                curveTo(17f, 16.929f, 17f, 17.394f, 16.923f, 17.78f)
                curveTo(16.608f, 19.367f, 15.367f, 20.608f, 13.78f, 20.923f)
                curveTo(13.394f, 21f, 12.929f, 21f, 12f, 21f)
                verticalLineTo(21f)
                curveTo(11.071f, 21f, 10.606f, 21f, 10.22f, 20.923f)
                curveTo(8.633f, 20.608f, 7.392f, 19.367f, 7.077f, 17.78f)
                curveTo(7f, 17.394f, 7f, 16.929f, 7f, 16f)
                verticalLineTo(14.333f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(9f, 9f)
                curveTo(9f, 8.068f, 9f, 7.602f, 9.152f, 7.235f)
                curveTo(9.355f, 6.745f, 9.745f, 6.355f, 10.235f, 6.152f)
                curveTo(10.602f, 6f, 11.068f, 6f, 12f, 6f)
                verticalLineTo(6f)
                curveTo(12.932f, 6f, 13.398f, 6f, 13.765f, 6.152f)
                curveTo(14.255f, 6.355f, 14.645f, 6.745f, 14.848f, 7.235f)
                curveTo(15f, 7.602f, 15f, 8.068f, 15f, 9f)
                verticalLineTo(11f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 11f)
                verticalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(15f, 3f)
                lineTo(13f, 6f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(9f, 3f)
                lineTo(11f, 6f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(20f, 9f)
                verticalLineTo(10f)
                curveTo(20f, 11.657f, 18.657f, 13f, 17f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(20f, 22f)
                curveTo(20f, 20.343f, 18.657f, 19f, 17f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(4f, 9f)
                verticalLineTo(10f)
                curveTo(4f, 11.657f, 5.343f, 13f, 7f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(4f, 22f)
                curveTo(4f, 20.343f, 5.343f, 19f, 7f, 19f)
            }
        }.build()

        return _Bug!!
    }

@Suppress("ObjectPropertyName")
private var _Bug: ImageVector? = null
