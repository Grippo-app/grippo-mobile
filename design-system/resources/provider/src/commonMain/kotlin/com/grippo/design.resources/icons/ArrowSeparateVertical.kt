package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ArrowSeparateVertical: ImageVector
    get() {
        if (_ArrowSeparateVertical != null) {
            return _ArrowSeparateVertical!!
        }
        _ArrowSeparateVertical = ImageVector.Builder(
            name = "ArrowSeparateVertical",
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
                moveTo(15.5f, 9.5f)
                lineTo(12f, 6f)
                lineTo(8.5f, 9.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 14f)
                lineTo(12f, 17.5f)
                lineTo(8.5f, 14f)
            }
        }.build()

        return _ArrowSeparateVertical!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowSeparateVertical: ImageVector? = null
