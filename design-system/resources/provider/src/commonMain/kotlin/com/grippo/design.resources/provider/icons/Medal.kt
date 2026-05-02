package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Medal: ImageVector
    get() {
        if (_Medal != null) {
            return _Medal!!
        }
        _Medal = ImageVector.Builder(
            name = "Medal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left ribbon — soft rounded ends
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(7f, 2f)
                curveTo(7.6f, 2f, 8.2f, 2.3f, 8.5f, 2.8f)
                lineTo(11.4f, 9.7f)
                curveTo(11.7f, 10.4f, 11.4f, 11.2f, 10.7f, 11.5f)
                curveTo(10f, 11.8f, 9.2f, 11.5f, 8.9f, 10.8f)
                lineTo(6f, 4f)
                curveTo(5.7f, 3.3f, 6.2f, 2.5f, 7f, 2f)
                close()
            }
            // Right ribbon — soft rounded ends
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(17f, 2f)
                curveTo(17.8f, 2.5f, 18.3f, 3.3f, 18f, 4f)
                lineTo(15.1f, 10.8f)
                curveTo(14.8f, 11.5f, 14f, 11.8f, 13.3f, 11.5f)
                curveTo(12.6f, 11.2f, 12.3f, 10.4f, 12.6f, 9.7f)
                lineTo(15.5f, 2.8f)
                curveTo(15.8f, 2.3f, 16.4f, 2f, 17f, 2f)
                close()
            }
            // Medal disc with star cut-out via EvenOdd
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer disc
                moveTo(12f, 9f)
                curveTo(8.134f, 9f, 5f, 12.134f, 5f, 16f)
                curveTo(5f, 19.866f, 8.134f, 23f, 12f, 23f)
                curveTo(15.866f, 23f, 19f, 19.866f, 19f, 16f)
                curveTo(19f, 12.134f, 15.866f, 9f, 12f, 9f)
                close()
                // Star cut-out (5-point with slight rounding via curve approximations)
                moveTo(12f, 12.5f)
                curveTo(12.2f, 12.5f, 12.4f, 12.6f, 12.5f, 12.8f)
                lineTo(13.3f, 14.5f)
                lineTo(15.2f, 14.7f)
                curveTo(15.4f, 14.7f, 15.5f, 14.8f, 15.6f, 15.0f)
                curveTo(15.6f, 15.2f, 15.6f, 15.4f, 15.4f, 15.5f)
                lineTo(14.0f, 16.8f)
                lineTo(14.4f, 18.7f)
                curveTo(14.4f, 18.9f, 14.4f, 19.1f, 14.2f, 19.2f)
                curveTo(14.0f, 19.3f, 13.8f, 19.3f, 13.7f, 19.2f)
                lineTo(12f, 18.2f)
                lineTo(10.3f, 19.2f)
                curveTo(10.2f, 19.3f, 10.0f, 19.3f, 9.8f, 19.2f)
                curveTo(9.6f, 19.1f, 9.6f, 18.9f, 9.6f, 18.7f)
                lineTo(10.0f, 16.8f)
                lineTo(8.6f, 15.5f)
                curveTo(8.4f, 15.4f, 8.4f, 15.2f, 8.4f, 15.0f)
                curveTo(8.5f, 14.8f, 8.6f, 14.7f, 8.8f, 14.7f)
                lineTo(10.7f, 14.5f)
                lineTo(11.5f, 12.8f)
                curveTo(11.6f, 12.6f, 11.8f, 12.5f, 12f, 12.5f)
                close()
            }
        }.build()

        return _Medal!!
    }

@Suppress("ObjectPropertyName")
private var _Medal: ImageVector? = null
