package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AntennaSignal: ImageVector
    get() {
        if (_AntennaSignal != null) {
            return _AntennaSignal!!
        }
        _AntennaSignal = ImageVector.Builder(
            name = "AntennaSignal",
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
                moveTo(17.5f, 8f)
                curveTo(17.5f, 8f, 19f, 9.5f, 19f, 12f)
                curveTo(19f, 14.5f, 17.5f, 16f, 17.5f, 16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 5f)
                curveTo(20.5f, 5f, 23f, 7.5f, 23f, 12f)
                curveTo(23f, 16.5f, 20.5f, 19f, 20.5f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 8f)
                curveTo(6.5f, 8f, 5f, 9.5f, 5f, 12f)
                curveTo(5f, 14.5f, 6.5f, 16f, 6.5f, 16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 5f)
                curveTo(3.5f, 5f, 1f, 7.5f, 1f, 12f)
                curveTo(1f, 16.5f, 3.5f, 19f, 3.5f, 19f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 13f)
                curveTo(12.552f, 13f, 13f, 12.552f, 13f, 12f)
                curveTo(13f, 11.448f, 12.552f, 11f, 12f, 11f)
                curveTo(11.448f, 11f, 11f, 11.448f, 11f, 12f)
                curveTo(11f, 12.552f, 11.448f, 13f, 12f, 13f)
                close()
            }
        }.build()

        return _AntennaSignal!!
    }

@Suppress("ObjectPropertyName")
private var _AntennaSignal: ImageVector? = null
