package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ControlSlider: ImageVector
    get() {
        if (_ControlSlider != null) {
            return _ControlSlider!!
        }
        _ControlSlider = ImageVector.Builder(
            name = "ControlSlider",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6.755f, 17.283f)
                lineTo(5.326f, 7.283f)
                curveTo(5.154f, 6.078f, 6.089f, 5f, 7.306f, 5f)
                horizontalLineTo(10.694f)
                curveTo(11.911f, 5f, 12.846f, 6.078f, 12.674f, 7.283f)
                lineTo(11.245f, 17.283f)
                curveTo(11.105f, 18.268f, 10.261f, 19f, 9.265f, 19f)
                horizontalLineTo(8.735f)
                curveTo(7.739f, 19f, 6.895f, 18.268f, 6.755f, 17.283f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 12f)
                horizontalLineTo(12f)
                moveTo(2f, 12f)
                horizontalLineTo(6f)
                horizontalLineTo(2f)
                close()
            }
        }.build()

        return _ControlSlider!!
    }

@Suppress("ObjectPropertyName")
private var _ControlSlider: ImageVector? = null
