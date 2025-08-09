package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AntennaSignalRounded: ImageVector
    get() {
        if (_AntennaSignalRounded != null) {
            return _AntennaSignalRounded!!
        }
        _AntennaSignalRounded = ImageVector.Builder(
            name = "AntennaSignalRounded",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 5.686f, 4.686f, 3f, 8f, 3f)
                horizontalLineTo(16f)
                curveTo(19.314f, 3f, 22f, 5.686f, 22f, 9f)
                verticalLineTo(15f)
                curveTo(22f, 18.314f, 19.314f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(4.686f, 21f, 2f, 18.314f, 2f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 9f)
                curveTo(15f, 9f, 16f, 10.125f, 16f, 12f)
                curveTo(16f, 13.875f, 15f, 15f, 15f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12.01f)
                lineTo(12.01f, 11.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 7f)
                curveTo(17f, 7f, 19f, 8.786f, 19f, 12f)
                curveTo(19f, 15.214f, 17f, 17f, 17f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                curveTo(9f, 9f, 8f, 10.125f, 8f, 12f)
                curveTo(8f, 13.875f, 9f, 15f, 9f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7f)
                curveTo(7f, 7f, 5f, 8.786f, 5f, 12f)
                curveTo(5f, 15.214f, 7f, 17f, 7f, 17f)
            }
        }.build()

        return _AntennaSignalRounded!!
    }

@Suppress("ObjectPropertyName")
private var _AntennaSignalRounded: ImageVector? = null
