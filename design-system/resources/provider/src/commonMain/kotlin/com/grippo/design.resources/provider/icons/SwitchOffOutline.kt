package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SwitchOffOutline: ImageVector
    get() {
        if (_SwitchOffOutline != null) {
            return _SwitchOffOutline!!
        }
        _SwitchOffOutline = ImageVector.Builder(
            name = "SwitchOffOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 13f)
                curveTo(7.552f, 13f, 8f, 12.552f, 8f, 12f)
                curveTo(8f, 11.448f, 7.552f, 11f, 7f, 11f)
                curveTo(6.448f, 11f, 6f, 11.448f, 6f, 12f)
                curveTo(6f, 12.552f, 6.448f, 13f, 7f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                curveTo(4.239f, 17f, 2f, 14.761f, 2f, 12f)
                curveTo(2f, 9.239f, 4.239f, 7f, 7f, 7f)
                horizontalLineTo(17f)
                curveTo(19.761f, 7f, 22f, 9.239f, 22f, 12f)
                curveTo(22f, 14.761f, 19.761f, 17f, 17f, 17f)
                close()
            }
        }.build()

        return _SwitchOffOutline!!
    }

@Suppress("ObjectPropertyName")
private var _SwitchOffOutline: ImageVector? = null
