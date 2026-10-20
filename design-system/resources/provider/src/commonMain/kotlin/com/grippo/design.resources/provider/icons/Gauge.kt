package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Gauge: ImageVector
    get() {
        if (_Gauge != null) {
            return _Gauge!!
        }
        _Gauge = ImageVector.Builder(
            name = "Gauge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 18f)
                verticalLineTo(17f)
                curveTo(3.5f, 12.306f, 7.306f, 8.5f, 12f, 8.5f)
                curveTo(16.694f, 8.5f, 20.5f, 12.306f, 20.5f, 17f)
                verticalLineTo(18f)
                moveTo(12f, 17f)
                lineTo(16f, 13f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 16f)
                curveTo(12.552f, 16f, 13f, 16.448f, 13f, 17f)
                curveTo(13f, 17.552f, 12.552f, 18f, 12f, 18f)
                curveTo(11.448f, 18f, 11f, 17.552f, 11f, 17f)
                curveTo(11f, 16.448f, 11.448f, 16f, 12f, 16f)
                close()
            }
        }.build()

        return _Gauge!!
    }

@Suppress("ObjectPropertyName")
private var _Gauge: ImageVector? = null
