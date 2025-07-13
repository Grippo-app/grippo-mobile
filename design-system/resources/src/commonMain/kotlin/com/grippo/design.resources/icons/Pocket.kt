package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Pocket: ImageVector
    get() {
        if (_Pocket != null) {
            return _Pocket!!
        }
        _Pocket = ImageVector.Builder(
            name = "Pocket",
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
                moveTo(21f, 6f)
                verticalLineTo(11f)
                curveTo(21f, 15.971f, 16.971f, 20f, 12f, 20f)
                curveTo(7.029f, 20f, 3f, 15.971f, 3f, 11f)
                verticalLineTo(6f)
                curveTo(3f, 4.895f, 3.895f, 4f, 5f, 4f)
                horizontalLineTo(19f)
                curveTo(20.105f, 4f, 21f, 4.895f, 21f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 10f)
                lineTo(12f, 14f)
                lineTo(16f, 10f)
            }
        }.build()

        return _Pocket!!
    }

@Suppress("ObjectPropertyName")
private var _Pocket: ImageVector? = null
