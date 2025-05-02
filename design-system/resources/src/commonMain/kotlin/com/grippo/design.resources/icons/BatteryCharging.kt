package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BatteryCharging: ImageVector
    get() {
        if (_BatteryCharging != null) {
            return _BatteryCharging!!
        }
        _BatteryCharging = ImageVector.Builder(
            name = "BatteryCharging",
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
                moveTo(15f, 6f)
                horizontalLineTo(17f)
                curveTo(17.53f, 6f, 18.039f, 6.211f, 18.414f, 6.586f)
                curveTo(18.789f, 6.961f, 19f, 7.47f, 19f, 8f)
                verticalLineTo(16f)
                curveTo(19f, 16.53f, 18.789f, 17.039f, 18.414f, 17.414f)
                curveTo(18.039f, 17.789f, 17.53f, 18f, 17f, 18f)
                horizontalLineTo(13.81f)
                moveTo(5f, 18f)
                horizontalLineTo(3f)
                curveTo(2.47f, 18f, 1.961f, 17.789f, 1.586f, 17.414f)
                curveTo(1.211f, 17.039f, 1f, 16.53f, 1f, 16f)
                verticalLineTo(8f)
                curveTo(1f, 7.47f, 1.211f, 6.961f, 1.586f, 6.586f)
                curveTo(1.961f, 6.211f, 2.47f, 6f, 3f, 6f)
                horizontalLineTo(6.19f)
                lineTo(5f, 18f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 13f)
                verticalLineTo(11f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 6f)
                lineTo(7f, 12f)
                horizontalLineTo(13f)
                lineTo(9f, 18f)
            }
        }.build()

        return _BatteryCharging!!
    }

@Suppress("ObjectPropertyName")
private var _BatteryCharging: ImageVector? = null
