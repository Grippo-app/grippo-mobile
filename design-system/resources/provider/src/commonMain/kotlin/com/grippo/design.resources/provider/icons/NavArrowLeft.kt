package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.NavArrowLeft: ImageVector
    get() {
        if (_NavArrowLeft != null) {
            return _NavArrowLeft!!
        }
        _NavArrowLeft = ImageVector.Builder(
            name = "NavArrowLeft",
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
                moveTo(15f, 6f)
                lineTo(9f, 12f)
                lineTo(15f, 18f)
            }
        }.build()

        return _NavArrowLeft!!
    }

@Suppress("ObjectPropertyName")
private var _NavArrowLeft: ImageVector? = null
