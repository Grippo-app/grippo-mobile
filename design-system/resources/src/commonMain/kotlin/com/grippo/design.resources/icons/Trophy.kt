package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Trophy: ImageVector
    get() {
        if (_Trophy != null) {
            return _Trophy!!
        }
        _Trophy = ImageVector.Builder(
            name = "Trophy",
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
                moveTo(6.745f, 4f)
                horizontalLineTo(17.313f)
                curveTo(17.313f, 4f, 16.433f, 17.257f, 12.029f, 17.257f)
                curveTo(9.878f, 17.257f, 8.568f, 14.094f, 7.79f, 10.857f)
                curveTo(6.976f, 7.468f, 6.745f, 4f, 6.745f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.313f, 4f)
                curveTo(17.313f, 4f, 18.234f, 3.017f, 19f, 3f)
                curveTo(20.5f, 2.966f, 20.777f, 4f, 20.777f, 4f)
                curveTo(21.071f, 4.61f, 21.306f, 6.194f, 19.897f, 7.657f)
                curveTo(18.487f, 9.12f, 16.91f, 10.4f, 16.268f, 10.857f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.745f, 4f)
                curveTo(6.745f, 4f, 5.786f, 3.006f, 5f, 3f)
                curveTo(3.5f, 2.988f, 3.223f, 4f, 3.223f, 4f)
                curveTo(2.929f, 4.61f, 2.694f, 6.194f, 4.103f, 7.657f)
                curveTo(5.512f, 9.12f, 7.148f, 10.4f, 7.79f, 10.857f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.507f, 20f)
                curveTo(8.507f, 18.171f, 12.029f, 17.257f, 12.029f, 17.257f)
                curveTo(12.029f, 17.257f, 15.552f, 18.171f, 15.552f, 20f)
                horizontalLineTo(8.507f)
                close()
            }
        }.build()

        return _Trophy!!
    }

@Suppress("ObjectPropertyName")
private var _Trophy: ImageVector? = null
