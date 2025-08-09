package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Pin: ImageVector
    get() {
        if (_Pin != null) {
            return _Pin!!
        }
        _Pin = ImageVector.Builder(
            name = "Pin",
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
                moveTo(9.5f, 14.5f)
                lineTo(3f, 21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 9.485f)
                lineTo(14.192f, 18.678f)
                lineTo(15.889f, 16.981f)
                lineTo(15.497f, 13.194f)
                lineTo(21.006f, 8.521f)
                lineTo(15.157f, 2.671f)
                lineTo(10.483f, 8.18f)
                lineTo(6.697f, 7.788f)
                lineTo(5f, 9.485f)
                close()
            }
        }.build()

        return _Pin!!
    }

@Suppress("ObjectPropertyName")
private var _Pin: ImageVector? = null
