package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Walking: ImageVector
    get() {
        if (_Walking != null) {
            return _Walking!!
        }
        _Walking = ImageVector.Builder(
            name = "Walking",
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
                moveTo(12.441f, 9.127f)
                lineTo(11.032f, 14.762f)
                lineTo(15.963f, 21.101f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.328f, 18.283f)
                lineTo(8.215f, 21.101f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.215f, 13.353f)
                curveTo(8.215f, 9.409f, 11.032f, 9.127f, 12.441f, 9.127f)
                lineTo(13.849f, 9.127f)
                curveTo(14.084f, 10.301f, 15.117f, 12.79f, 17.371f, 13.353f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 7f)
                curveTo(14.105f, 7f, 15f, 6.105f, 15f, 5f)
                curveTo(15f, 3.895f, 14.105f, 3f, 13f, 3f)
                curveTo(11.895f, 3f, 11f, 3.895f, 11f, 5f)
                curveTo(11f, 6.105f, 11.895f, 7f, 13f, 7f)
                close()
            }
        }.build()

        return _Walking!!
    }

@Suppress("ObjectPropertyName")
private var _Walking: ImageVector? = null
