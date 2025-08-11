package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Egg: ImageVector
    get() {
        if (_Egg != null) {
            return _Egg!!
        }
        _Egg = ImageVector.Builder(
            name = "Egg",
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
                moveTo(12f, 22f)
                curveTo(16.418f, 22f, 20f, 18.418f, 20f, 14f)
                curveTo(20f, 9.582f, 16.418f, 2f, 12f, 2f)
                curveTo(7.582f, 2f, 4f, 9.582f, 4f, 14f)
                curveTo(4f, 18.418f, 7.582f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _Egg!!
    }

@Suppress("ObjectPropertyName")
private var _Egg: ImageVector? = null
