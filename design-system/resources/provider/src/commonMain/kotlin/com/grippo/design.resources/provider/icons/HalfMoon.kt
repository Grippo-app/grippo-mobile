package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.HalfMoon: ImageVector
    get() {
        if (_HalfMoon != null) {
            return _HalfMoon!!
        }
        _HalfMoon = ImageVector.Builder(
            name = "HalfMoon",
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
                moveTo(3f, 11.507f)
                curveTo(3f, 16.75f, 7.25f, 21f, 12.493f, 21f)
                curveTo(16.221f, 21f, 19.447f, 18.852f, 21f, 15.726f)
                curveTo(12.493f, 15.726f, 8.274f, 11.507f, 8.274f, 3f)
                curveTo(5.148f, 4.553f, 3f, 7.779f, 3f, 11.507f)
                close()
            }
        }.build()

        return _HalfMoon!!
    }

@Suppress("ObjectPropertyName")
private var _HalfMoon: ImageVector? = null
