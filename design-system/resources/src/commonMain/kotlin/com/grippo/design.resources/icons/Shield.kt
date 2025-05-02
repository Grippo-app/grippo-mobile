package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Shield: ImageVector
    get() {
        if (_Shield != null) {
            return _Shield!!
        }
        _Shield = ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(12f, 22f, 20f, 18f, 20f, 12f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(12f)
                curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _Shield!!
    }

@Suppress("ObjectPropertyName")
private var _Shield: ImageVector? = null
