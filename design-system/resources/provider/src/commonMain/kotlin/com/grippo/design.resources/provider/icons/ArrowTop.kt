package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ArrowTop: ImageVector
    get() {
        if (_ArrowTop != null) {
            return _ArrowTop!!
        }
        _ArrowTop = ImageVector.Builder(
            name = "ArrowTop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(18f, 15f)
                lineTo(12f, 9f)
                lineTo(6f, 15f)
            }
        }.build()

        return _ArrowTop!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowTop: ImageVector? = null


