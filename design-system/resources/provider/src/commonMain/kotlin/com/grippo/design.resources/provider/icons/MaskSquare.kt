package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MaskSquare: ImageVector
    get() {
        if (_MaskSquare != null) {
            return _MaskSquare!!
        }
        _MaskSquare = ImageVector.Builder(
            name = "MaskSquare",
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
                moveTo(21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 17.659f)
                lineTo(14f, 6.341f)
                moveTo(10f, 17.659f)
                curveTo(10.626f, 17.88f, 11.299f, 18f, 12f, 18f)
                curveTo(15.314f, 18f, 18f, 15.314f, 18f, 12f)
                curveTo(18f, 9.388f, 16.33f, 7.165f, 14f, 6.341f)
                lineTo(10f, 17.659f)
                close()
                moveTo(10f, 17.659f)
                curveTo(7.67f, 16.835f, 6f, 14.612f, 6f, 12f)
                curveTo(6f, 8.686f, 8.686f, 6f, 12f, 6f)
                curveTo(12.701f, 6f, 13.374f, 6.12f, 14f, 6.341f)
                lineTo(10f, 17.659f)
                close()
            }
        }.build()

        return _MaskSquare!!
    }

@Suppress("ObjectPropertyName")
private var _MaskSquare: ImageVector? = null
