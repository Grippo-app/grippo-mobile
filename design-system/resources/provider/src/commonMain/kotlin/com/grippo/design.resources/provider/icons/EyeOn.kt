package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOn: ImageVector
    get() {
        if (_EyeOn != null) {
            return _EyeOn!!
        }
        _EyeOn = ImageVector.Builder(
            name = "EyeOn",
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
                moveTo(3f, 12f)
                curveTo(4.7f, 8.1f, 8f, 6f, 12f, 6f)
                curveTo(16f, 6f, 19.3f, 8.1f, 21f, 12f)
                curveTo(19.3f, 15.9f, 16f, 18f, 12f, 18f)
                curveTo(8f, 18f, 4.7f, 15.9f, 3f, 12f)
                close()
                moveTo(12f, 9f)
                curveTo(13.657f, 9f, 15f, 10.343f, 15f, 12f)
                curveTo(15f, 13.657f, 13.657f, 15f, 12f, 15f)
                curveTo(10.343f, 15f, 9f, 13.657f, 9f, 12f)
                curveTo(9f, 10.343f, 10.343f, 9f, 12f, 9f)
                close()
            }
        }.build()

        return _EyeOn!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOn: ImageVector? = null
