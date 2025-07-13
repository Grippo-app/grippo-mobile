package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Edit: ImageVector
    get() {
        if (_Edit != null) {
            return _Edit!!
        }
        _Edit = ImageVector.Builder(
            name = "Edit",
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
                moveTo(3f, 21f)
                horizontalLineTo(12f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.222f, 5.828f)
                lineTo(17.171f, 10.778f)
                moveTo(12.222f, 5.828f)
                lineTo(15.05f, 3f)
                lineTo(20f, 7.95f)
                lineTo(17.171f, 10.778f)
                lineTo(12.222f, 5.828f)
                close()
                moveTo(12.222f, 5.828f)
                lineTo(6.615f, 11.435f)
                curveTo(6.428f, 11.623f, 6.322f, 11.877f, 6.322f, 12.142f)
                verticalLineTo(16.678f)
                horizontalLineTo(10.858f)
                curveTo(11.123f, 16.678f, 11.377f, 16.572f, 11.565f, 16.385f)
                lineTo(17.171f, 10.778f)
                lineTo(12.222f, 5.828f)
                close()
            }
        }.build()

        return _Edit!!
    }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null
