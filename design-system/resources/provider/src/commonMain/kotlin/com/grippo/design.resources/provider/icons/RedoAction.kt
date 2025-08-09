package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.RedoAction: ImageVector
    get() {
        if (_RedoAction != null) {
            return _RedoAction!!
        }
        _RedoAction = ImageVector.Builder(
            name = "RedoAction",
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
                moveTo(19f, 7f)
                verticalLineTo(9.5f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 9.5f)
                curveTo(16f, 9.5f, 12f, 9.5f, 9f, 9.5f)
                curveTo(3.5f, 9.5f, 3.5f, 18f, 9f, 18f)
                curveTo(12f, 18f, 19f, 18f, 19f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.5f, 13f)
                lineTo(16f, 9.5f)
                lineTo(12.5f, 6f)
            }
        }.build()

        return _RedoAction!!
    }

@Suppress("ObjectPropertyName")
private var _RedoAction: ImageVector? = null
