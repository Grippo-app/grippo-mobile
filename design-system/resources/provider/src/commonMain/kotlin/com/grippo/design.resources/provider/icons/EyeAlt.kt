package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeAlt: ImageVector
    get() {
        if (_EyeAlt != null) {
            return _EyeAlt!!
        }
        _EyeAlt = ImageVector.Builder(
            name = "EyeAlt",
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
                moveTo(4.5f, 12.5f)
                curveTo(7.5f, 6f, 16.5f, 6f, 19.5f, 12.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 16f)
                curveTo(10.895f, 16f, 10f, 15.105f, 10f, 14f)
                curveTo(10f, 12.895f, 10.895f, 12f, 12f, 12f)
                curveTo(13.105f, 12f, 14f, 12.895f, 14f, 14f)
                curveTo(14f, 15.105f, 13.105f, 16f, 12f, 16f)
                close()
            }
        }.build()

        return _EyeAlt!!
    }

@Suppress("ObjectPropertyName")
private var _EyeAlt: ImageVector? = null
