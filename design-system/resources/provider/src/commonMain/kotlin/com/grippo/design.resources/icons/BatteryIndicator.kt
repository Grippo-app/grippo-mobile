package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BatteryIndicator: ImageVector
    get() {
        if (_BatteryIndicator != null) {
            return _BatteryIndicator!!
        }
        _BatteryIndicator = ImageVector.Builder(
            name = "BatteryIndicator",
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
                moveTo(14f, 13f)
                horizontalLineTo(16f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 13f)
                verticalLineTo(15f)
                moveTo(6f, 13f)
                horizontalLineTo(8f)
                horizontalLineTo(6f)
                close()
                moveTo(10f, 13f)
                horizontalLineTo(8f)
                horizontalLineTo(10f)
                close()
                moveTo(8f, 13f)
                verticalLineTo(11f)
                verticalLineTo(13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 7f)
                horizontalLineTo(18f)
                moveTo(6f, 7f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 7f, 2f, 7.269f, 2f, 7.6f)
                verticalLineTo(18.4f)
                curveTo(2f, 18.731f, 2.269f, 19f, 2.6f, 19f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 19f, 22f, 18.731f, 22f, 18.4f)
                verticalLineTo(7.6f)
                curveTo(22f, 7.269f, 21.731f, 7f, 21.4f, 7f)
                horizontalLineTo(18f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 7f)
                horizontalLineTo(8f)
                horizontalLineTo(6f)
                close()
                moveTo(8f, 7f)
                horizontalLineTo(16f)
                horizontalLineTo(8f)
                close()
                moveTo(16f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(18f)
                verticalLineTo(7f)
                horizontalLineTo(16f)
                close()
            }
        }.build()

        return _BatteryIndicator!!
    }

@Suppress("ObjectPropertyName")
private var _BatteryIndicator: ImageVector? = null
