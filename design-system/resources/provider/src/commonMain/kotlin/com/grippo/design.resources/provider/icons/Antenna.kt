package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Antenna: ImageVector
    get() {
        if (_Antenna != null) {
            return _Antenna!!
        }
        _Antenna = ImageVector.Builder(
            name = "Antenna",
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
                moveTo(16f, 1f)
                curveTo(16f, 1f, 17.5f, 2f, 17.5f, 4f)
                curveTo(17.5f, 6f, 16f, 7f, 16f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 1f)
                curveTo(8f, 1f, 6.5f, 2f, 6.5f, 4f)
                curveTo(6.5f, 6f, 8f, 7f, 8f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.111f, 19f)
                horizontalLineTo(15.889f)
                moveTo(7f, 23f)
                lineTo(8.111f, 19f)
                lineTo(7f, 23f)
                close()
                moveTo(17f, 23f)
                lineTo(15.889f, 19f)
                lineTo(17f, 23f)
                close()
                moveTo(14.5f, 14f)
                lineTo(12f, 5f)
                lineTo(9.5f, 14f)
                horizontalLineTo(14.5f)
                close()
                moveTo(14.5f, 14f)
                horizontalLineTo(9.5f)
                horizontalLineTo(14.5f)
                close()
                moveTo(14.5f, 14f)
                lineTo(15.889f, 19f)
                lineTo(14.5f, 14f)
                close()
                moveTo(9.5f, 14f)
                lineTo(8.111f, 19f)
                lineTo(9.5f, 14f)
                close()
            }
        }.build()

        return _Antenna!!
    }

@Suppress("ObjectPropertyName")
private var _Antenna: ImageVector? = null
