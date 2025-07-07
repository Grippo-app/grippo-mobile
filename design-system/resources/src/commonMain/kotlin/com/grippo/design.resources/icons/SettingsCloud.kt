package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.SettingsCloud: ImageVector
    get() {
        if (_SettingsCloud != null) {
            return _SettingsCloud!!
        }
        _SettingsCloud = ImageVector.Builder(
            name = "SettingsCloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 8f)
                curveTo(8.727f, 8f, 8.727f, 10f, 8.727f, 11f)
                curveTo(7.818f, 11f, 6f, 11.5f, 6f, 13.5f)
                curveTo(6f, 15.5f, 7.818f, 16f, 8.727f, 16f)
                horizontalLineTo(15.273f)
                curveTo(16.182f, 16f, 18f, 15.5f, 18f, 13.5f)
                curveTo(18f, 11.5f, 16.182f, 11f, 15.273f, 11f)
                curveTo(15.273f, 10f, 15.273f, 8f, 12f, 8f)
                close()
            }
        }.build()

        return _SettingsCloud!!
    }

@Suppress("ObjectPropertyName")
private var _SettingsCloud: ImageVector? = null
