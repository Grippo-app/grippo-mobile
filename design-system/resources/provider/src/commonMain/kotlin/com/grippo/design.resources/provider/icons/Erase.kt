package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Erase: ImageVector
    get() {
        if (_Erase != null) {
            return _Erase!!
        }
        _Erase = ImageVector.Builder(
            name = "Erase",
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
                moveTo(21f, 21f)
                horizontalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.889f, 14.889f)
                lineTo(8.464f, 7.465f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.893f, 12.606f)
                lineTo(12.086f, 3.414f)
                curveTo(12.867f, 2.633f, 14.133f, 2.633f, 14.914f, 3.414f)
                lineTo(19.864f, 8.364f)
                curveTo(20.645f, 9.145f, 20.645f, 10.411f, 19.864f, 11.192f)
                lineTo(10.621f, 20.435f)
                curveTo(10.26f, 20.797f, 9.769f, 21f, 9.257f, 21f)
                curveTo(8.746f, 21f, 8.255f, 20.797f, 7.893f, 20.435f)
                lineTo(2.893f, 15.435f)
                curveTo(2.112f, 14.654f, 2.112f, 13.388f, 2.893f, 12.606f)
                close()
            }
        }.build()

        return _Erase!!
    }

@Suppress("ObjectPropertyName")
private var _Erase: ImageVector? = null
