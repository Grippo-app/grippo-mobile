package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LongArrowUpLeft: ImageVector
    get() {
        if (_LongArrowUpLeft != null) {
            return _LongArrowUpLeft!!
        }
        _LongArrowUpLeft = ImageVector.Builder(
            name = "LongArrowUpLeft",
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
                moveTo(10.25f, 4.75f)
                lineTo(6.75f, 8.25f)
                lineTo(10.25f, 11.75f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.75f, 8.25f)
                horizontalLineTo(12.75f)
                curveTo(14.959f, 8.25f, 16.75f, 10.041f, 16.75f, 12.25f)
                verticalLineTo(19.25f)
            }
        }.build()

        return _LongArrowUpLeft!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowUpLeft: ImageVector? = null
