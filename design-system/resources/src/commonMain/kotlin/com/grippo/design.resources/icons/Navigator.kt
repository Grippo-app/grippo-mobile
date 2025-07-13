package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Navigator: ImageVector
    get() {
        if (_Navigator != null) {
            return _Navigator!!
        }
        _Navigator = ImageVector.Builder(
            name = "Navigator",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(17.873f, 15.475f)
                curveTo(18.334f, 16.345f, 17.436f, 17.306f, 16.537f, 16.907f)
                lineTo(11.999f, 14.89f)
                lineTo(7.462f, 16.907f)
                curveTo(6.563f, 17.306f, 5.665f, 16.345f, 6.125f, 15.475f)
                lineTo(11.084f, 6.11f)
                curveTo(11.473f, 5.375f, 12.526f, 5.375f, 12.915f, 6.11f)
                lineTo(17.873f, 15.475f)
                close()
            }
        }.build()

        return _Navigator!!
    }

@Suppress("ObjectPropertyName")
private var _Navigator: ImageVector? = null
