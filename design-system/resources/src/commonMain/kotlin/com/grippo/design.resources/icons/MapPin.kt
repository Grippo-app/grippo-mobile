package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MapPin: ImageVector
    get() {
        if (_MapPin != null) {
            return _MapPin!!
        }
        _MapPin = ImageVector.Builder(
            name = "MapPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 10f)
                curveTo(21f, 17f, 12f, 23f, 12f, 23f)
                curveTo(12f, 23f, 3f, 17f, 3f, 10f)
                curveTo(3f, 7.613f, 3.948f, 5.324f, 5.636f, 3.636f)
                curveTo(7.324f, 1.948f, 9.613f, 1f, 12f, 1f)
                curveTo(14.387f, 1f, 16.676f, 1.948f, 18.364f, 3.636f)
                curveTo(20.052f, 5.324f, 21f, 7.613f, 21f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 13f)
                curveTo(13.657f, 13f, 15f, 11.657f, 15f, 10f)
                curveTo(15f, 8.343f, 13.657f, 7f, 12f, 7f)
                curveTo(10.343f, 7f, 9f, 8.343f, 9f, 10f)
                curveTo(9f, 11.657f, 10.343f, 13f, 12f, 13f)
                close()
            }
        }.build()

        return _MapPin!!
    }

@Suppress("ObjectPropertyName")
private var _MapPin: ImageVector? = null
