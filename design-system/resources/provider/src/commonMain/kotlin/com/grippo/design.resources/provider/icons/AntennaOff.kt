package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AntennaOff: ImageVector
    get() {
        if (_AntennaOff != null) {
            return _AntennaOff!!
        }
        _AntennaOff = ImageVector.Builder(
            name = "AntennaOff",
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
                moveTo(12f, 5f)
                curveTo(12.552f, 5f, 13f, 4.552f, 13f, 4f)
                curveTo(13f, 3.448f, 12.552f, 3f, 12f, 3f)
                curveTo(11.448f, 3f, 11f, 3.448f, 11f, 4f)
                curveTo(11f, 4.552f, 11.448f, 5f, 12f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.444f, 7f)
                lineTo(12f, 5f)
                lineTo(13.047f, 8.768f)
                moveTo(7f, 23f)
                lineTo(8.111f, 19f)
                lineTo(7f, 23f)
                close()
                moveTo(17f, 23f)
                lineTo(15.889f, 19f)
                lineTo(17f, 23f)
                close()
                moveTo(9.5f, 14f)
                lineTo(8.111f, 19f)
                lineTo(9.5f, 14f)
                close()
                moveTo(9.5f, 14f)
                horizontalLineTo(13.5f)
                horizontalLineTo(9.5f)
                close()
                moveTo(9.5f, 14f)
                lineTo(10.3f, 11.121f)
                lineTo(9.5f, 14f)
                close()
                moveTo(8.111f, 19f)
                horizontalLineTo(15.889f)
                horizontalLineTo(8.111f)
                close()
                moveTo(15.889f, 19f)
                lineTo(14.705f, 14.736f)
                lineTo(15.889f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }.build()

        return _AntennaOff!!
    }

@Suppress("ObjectPropertyName")
private var _AntennaOff: ImageVector? = null
