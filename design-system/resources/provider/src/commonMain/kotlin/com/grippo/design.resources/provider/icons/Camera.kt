package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Camera: ImageVector
    get() {
        if (_Camera != null) {
            return _Camera!!
        }
        _Camera = ImageVector.Builder(
            name = "Camera",
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
                moveTo(2f, 19f)
                verticalLineTo(9f)
                curveTo(2f, 7.895f, 2.895f, 7f, 4f, 7f)
                horizontalLineTo(4.5f)
                curveTo(5.13f, 7f, 5.722f, 6.704f, 6.1f, 6.2f)
                lineTo(8.32f, 3.24f)
                curveTo(8.433f, 3.089f, 8.611f, 3f, 8.8f, 3f)
                horizontalLineTo(15.2f)
                curveTo(15.389f, 3f, 15.567f, 3.089f, 15.68f, 3.24f)
                lineTo(17.9f, 6.2f)
                curveTo(18.278f, 6.704f, 18.871f, 7f, 19.5f, 7f)
                horizontalLineTo(20f)
                curveTo(21.105f, 7f, 22f, 7.895f, 22f, 9f)
                verticalLineTo(19f)
                curveTo(22f, 20.105f, 21.105f, 21f, 20f, 21f)
                horizontalLineTo(4f)
                curveTo(2.895f, 21f, 2f, 20.105f, 2f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                curveTo(14.209f, 17f, 16f, 15.209f, 16f, 13f)
                curveTo(16f, 10.791f, 14.209f, 9f, 12f, 9f)
                curveTo(9.791f, 9f, 8f, 10.791f, 8f, 13f)
                curveTo(8f, 15.209f, 9.791f, 17f, 12f, 17f)
                close()
            }
        }.build()

        return _Camera!!
    }

@Suppress("ObjectPropertyName")
private var _Camera: ImageVector? = null
