package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EaseInOut: ImageVector
    get() {
        if (_EaseInOut != null) {
            return _EaseInOut!!
        }
        _EaseInOut = ImageVector.Builder(
            name = "EaseInOut",
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
                moveTo(3f, 20f)
                curveTo(11f, 20f, 13f, 4f, 21f, 4f)
            }
        }.build()

        return _EaseInOut!!
    }

@Suppress("ObjectPropertyName")
private var _EaseInOut: ImageVector? = null
