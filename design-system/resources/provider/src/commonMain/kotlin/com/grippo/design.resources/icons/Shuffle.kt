package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Shuffle: ImageVector
    get() {
        if (_Shuffle != null) {
            return _Shuffle!!
        }
        _Shuffle = ImageVector.Builder(
            name = "Shuffle",
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
                moveTo(22f, 7f)
                curveTo(19f, 7f, 13.5f, 7f, 11.5f, 12.5f)
                curveTo(9.5f, 18f, 5f, 18f, 2f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 5f)
                curveTo(20f, 5f, 21.219f, 6.219f, 22f, 7f)
                curveTo(21.219f, 7.781f, 20f, 9f, 20f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 18f)
                curveTo(19f, 18f, 13.5f, 18f, 11.5f, 12.5f)
                curveTo(9.5f, 7f, 5f, 7f, 2f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 20f)
                curveTo(20f, 20f, 21.219f, 18.781f, 22f, 18f)
                curveTo(21.219f, 17.219f, 20f, 16f, 20f, 16f)
            }
        }.build()

        return _Shuffle!!
    }

@Suppress("ObjectPropertyName")
private var _Shuffle: ImageVector? = null
