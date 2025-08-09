package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BellNotification: ImageVector
    get() {
        if (_BellNotification != null) {
            return _BellNotification!!
        }
        _BellNotification = ImageVector.Builder(
            name = "BellNotification",
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
                moveTo(18.134f, 11f)
                curveTo(18.715f, 16.375f, 21f, 18f, 21f, 18f)
                horizontalLineTo(3f)
                curveTo(3f, 18f, 6f, 15.867f, 6f, 8.4f)
                curveTo(6f, 6.703f, 6.632f, 5.075f, 7.757f, 3.875f)
                curveTo(8.883f, 2.674f, 10.409f, 2f, 12f, 2f)
                curveTo(12.337f, 2f, 12.672f, 2.03f, 13f, 2.089f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 8f)
                curveTo(20.657f, 8f, 22f, 6.657f, 22f, 5f)
                curveTo(22f, 3.343f, 20.657f, 2f, 19f, 2f)
                curveTo(17.343f, 2f, 16f, 3.343f, 16f, 5f)
                curveTo(16f, 6.657f, 17.343f, 8f, 19f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.73f, 21f)
                curveTo(13.554f, 21.303f, 13.302f, 21.555f, 12.998f, 21.729f)
                curveTo(12.695f, 21.904f, 12.35f, 21.997f, 12f, 21.997f)
                curveTo(11.65f, 21.997f, 11.305f, 21.904f, 11.002f, 21.729f)
                curveTo(10.698f, 21.555f, 10.446f, 21.303f, 10.27f, 21f)
            }
        }.build()

        return _BellNotification!!
    }

@Suppress("ObjectPropertyName")
private var _BellNotification: ImageVector? = null
