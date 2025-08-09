package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeEmpty: ImageVector
    get() {
        if (_EyeEmpty != null) {
            return _EyeEmpty!!
        }
        _EyeEmpty = ImageVector.Builder(
            name = "EyeEmpty",
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
                moveTo(12f, 14f)
                curveTo(13.105f, 14f, 14f, 13.105f, 14f, 12f)
                curveTo(14f, 10.895f, 13.105f, 10f, 12f, 10f)
                curveTo(10.895f, 10f, 10f, 10.895f, 10f, 12f)
                curveTo(10f, 13.105f, 10.895f, 14f, 12f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                curveTo(19.111f, 14.991f, 15.718f, 18f, 12f, 18f)
                curveTo(8.282f, 18f, 4.889f, 14.991f, 3f, 12f)
                curveTo(5.299f, 9.158f, 7.992f, 6f, 12f, 6f)
                curveTo(16.008f, 6f, 18.701f, 9.158f, 21f, 12f)
                close()
            }
        }.build()

        return _EyeEmpty!!
    }

@Suppress("ObjectPropertyName")
private var _EyeEmpty: ImageVector? = null
