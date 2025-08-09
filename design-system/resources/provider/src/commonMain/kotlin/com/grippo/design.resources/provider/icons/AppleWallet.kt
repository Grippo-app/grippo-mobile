package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AppleWallet: ImageVector
    get() {
        if (_AppleWallet != null) {
            return _AppleWallet!!
        }
        _AppleWallet = ImageVector.Builder(
            name = "AppleWallet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(19f)
                curveTo(21f, 20.105f, 20.105f, 21f, 19f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 15f)
                horizontalLineTo(9.4f)
                curveTo(9.731f, 15f, 10.005f, 15.278f, 10.15f, 15.576f)
                curveTo(10.356f, 15.999f, 10.844f, 16.5f, 12f, 16.5f)
                curveTo(13.156f, 16.5f, 13.644f, 15.999f, 13.85f, 15.576f)
                curveTo(13.995f, 15.278f, 14.269f, 15f, 14.6f, 15f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 7f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 11f)
                horizontalLineTo(21f)
            }
        }.build()

        return _AppleWallet!!
    }

@Suppress("ObjectPropertyName")
private var _AppleWallet: ImageVector? = null
