package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FlagUa: ImageVector
    get() {
        if (_FlagUa != null) return _FlagUa!!
        _FlagUa = ImageVector.Builder(
            name = "FlagUa",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Circle background (blue)
            path(fill = SolidColor(Color(0xFF005BBB))) {
                moveTo(12f, 2f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, 20f)
                arcToRelative(10f, 10f, 0f, true, true, 0f, -20f)
                close()
            }
            // Bottom half (yellow) as a semicircle clipped by arc
            path(fill = SolidColor(Color(0xFFFFD500))) {
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f, false, true, 20f, 0f) // left → right along bottom arc
                lineTo(2f, 12f) // close via diameter chord
                close()
            }
        }.build()
        return _FlagUa!!
    }

@Suppress("ObjectPropertyName")
private var _FlagUa: ImageVector? = null