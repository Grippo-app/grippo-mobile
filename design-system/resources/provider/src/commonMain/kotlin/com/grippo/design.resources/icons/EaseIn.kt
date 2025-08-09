package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EaseIn: ImageVector
    get() {
        if (_EaseIn != null) {
            return _EaseIn!!
        }
        _EaseIn = ImageVector.Builder(
            name = "EaseIn",
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
                curveTo(11f, 20f, 21f, 4f, 21f, 4f)
            }
        }.build()

        return _EaseIn!!
    }

@Suppress("ObjectPropertyName")
private var _EaseIn: ImageVector? = null
