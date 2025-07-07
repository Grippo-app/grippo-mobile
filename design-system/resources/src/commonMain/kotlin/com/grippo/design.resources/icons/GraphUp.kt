package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.GraphUp: ImageVector
    get() {
        if (_GraphUp != null) {
            return _GraphUp!!
        }
        _GraphUp = ImageVector.Builder(
            name = "GraphUp",
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
                moveTo(20f, 20f)
                horizontalLineTo(4f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 16.5f)
                lineTo(12f, 9f)
                lineTo(15f, 12f)
                lineTo(19.5f, 7.5f)
            }
        }.build()

        return _GraphUp!!
    }

@Suppress("ObjectPropertyName")
private var _GraphUp: ImageVector? = null
