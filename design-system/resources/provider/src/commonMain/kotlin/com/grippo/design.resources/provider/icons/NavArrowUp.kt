package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.NavArrowUp: ImageVector
    get() {
        if (_NavArrowUp != null) {
            return _NavArrowUp!!
        }
        _NavArrowUp = ImageVector.Builder(
            name = "NavArrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 15f)
                lineTo(12f, 9f)
                lineTo(18f, 15f)
            }
        }.build()

        return _NavArrowUp!!
    }

@Suppress("ObjectPropertyName")
private var _NavArrowUp: ImageVector? = null
