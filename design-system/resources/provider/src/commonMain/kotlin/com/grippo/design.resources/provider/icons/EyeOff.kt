package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOff: ImageVector
    get() {
        if (_EyeOff != null) {
            return _EyeOff!!
        }
        _EyeOff = ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.2f, 6.3f)
                curveTo(10.1f, 6.1f, 11.1f, 6f, 12f, 6f)
                curveTo(16f, 6f, 19.3f, 8.1f, 21f, 12f)
                curveTo(20.4f, 13.1f, 19.6f, 14.1f, 18.7f, 14.9f)
                moveTo(14.8f, 17.7f)
                curveTo(13.9f, 17.9f, 12.9f, 18f, 12f, 18f)
                curveTo(8f, 18f, 4.7f, 15.9f, 3f, 12f)
                curveTo(3.6f, 10.9f, 4.4f, 9.9f, 5.3f, 9.1f)
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }.build()

        return _EyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOff: ImageVector? = null
