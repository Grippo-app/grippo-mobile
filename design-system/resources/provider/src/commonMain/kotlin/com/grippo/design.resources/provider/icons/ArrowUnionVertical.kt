package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ArrowUnionVertical: ImageVector
    get() {
        if (_ArrowUnionVertical != null) {
            return _ArrowUnionVertical!!
        }
        _ArrowUnionVertical = ImageVector.Builder(
            name = "ArrowUnionVertical",
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
                moveTo(15.5f, 6f)
                lineTo(12f, 9.5f)
                lineTo(8.5f, 6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 17.5f)
                lineTo(12f, 14f)
                lineTo(8.5f, 17.5f)
            }
        }.build()

        return _ArrowUnionVertical!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowUnionVertical: ImageVector? = null
