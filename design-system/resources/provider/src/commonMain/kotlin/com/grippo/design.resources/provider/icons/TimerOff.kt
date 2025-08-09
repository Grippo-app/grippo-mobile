package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.TimerOff: ImageVector
    get() {
        if (_TimerOff != null) {
            return _TimerOff!!
        }
        _TimerOff = ImageVector.Builder(
            name = "TimerOff",
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
                moveTo(9f, 2f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 7f)
                lineTo(19f, 21.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 10f)
                verticalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.419f, 17f)
                curveTo(19.794f, 16.074f, 20f, 15.061f, 20f, 14f)
                curveTo(20f, 9.582f, 16.418f, 6f, 12f, 6f)
                curveTo(11.019f, 6f, 10.079f, 6.177f, 9.21f, 6.5f)
                moveTo(6.19f, 8.5f)
                curveTo(4.833f, 9.934f, 4f, 11.87f, 4f, 14f)
                curveTo(4f, 18.418f, 7.582f, 22f, 12f, 22f)
                curveTo(14.005f, 22f, 15.837f, 21.263f, 17.241f, 20.044f)
                lineTo(6.19f, 8.5f)
                close()
            }
        }.build()

        return _TimerOff!!
    }

@Suppress("ObjectPropertyName")
private var _TimerOff: ImageVector? = null
