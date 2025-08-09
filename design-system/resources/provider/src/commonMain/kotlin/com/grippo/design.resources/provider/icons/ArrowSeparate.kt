package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ArrowSeparate: ImageVector
    get() {
        if (_ArrowSeparate != null) {
            return _ArrowSeparate!!
        }
        _ArrowSeparate = ImageVector.Builder(
            name = "ArrowSeparate",
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
                moveTo(9.5f, 8f)
                lineTo(6f, 11.5f)
                lineTo(9.5f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 8f)
                lineTo(17.5f, 11.5f)
                lineTo(14f, 15f)
            }
        }.build()

        return _ArrowSeparate!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowSeparate: ImageVector? = null
