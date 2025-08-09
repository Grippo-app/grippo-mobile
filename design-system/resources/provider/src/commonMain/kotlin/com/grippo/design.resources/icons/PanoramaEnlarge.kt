package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PanoramaEnlarge: ImageVector
    get() {
        if (_PanoramaEnlarge != null) {
            return _PanoramaEnlarge!!
        }
        _PanoramaEnlarge = ImageVector.Builder(
            name = "PanoramaEnlarge",
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
                moveTo(12f, 5f)
                curveTo(14.995f, 5f, 19.235f, 5.692f, 20.576f, 5.925f)
                curveTo(20.831f, 5.969f, 21.024f, 6.171f, 21.056f, 6.428f)
                curveTo(21.187f, 7.456f, 21.5f, 10.119f, 21.5f, 12f)
                curveTo(21.5f, 13.881f, 21.187f, 16.544f, 21.056f, 17.572f)
                curveTo(21.024f, 17.829f, 20.831f, 18.031f, 20.576f, 18.075f)
                curveTo(19.235f, 18.308f, 14.995f, 19f, 12f, 19f)
                curveTo(9.005f, 19f, 4.765f, 18.308f, 3.424f, 18.075f)
                curveTo(3.169f, 18.031f, 2.977f, 17.829f, 2.944f, 17.572f)
                curveTo(2.813f, 16.544f, 2.5f, 13.881f, 2.5f, 12f)
                curveTo(2.5f, 10.119f, 2.813f, 7.456f, 2.944f, 6.428f)
                curveTo(2.977f, 6.171f, 3.169f, 5.969f, 3.424f, 5.925f)
                curveTo(4.765f, 5.692f, 9.005f, 5f, 12f, 5f)
                close()
            }
        }.build()

        return _PanoramaEnlarge!!
    }

@Suppress("ObjectPropertyName")
private var _PanoramaEnlarge: ImageVector? = null
