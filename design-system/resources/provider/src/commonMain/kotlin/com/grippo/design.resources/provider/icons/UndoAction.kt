package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.UndoAction: ImageVector
    get() {
        if (_UndoAction != null) {
            return _UndoAction!!
        }
        _UndoAction = ImageVector.Builder(
            name = "UndoAction",
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
                moveTo(5f, 7f)
                verticalLineTo(9.5f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.875f, 9.5f)
                horizontalLineTo(14.875f)
                curveTo(20.375f, 9.5f, 20.375f, 18f, 14.875f, 18f)
                curveTo(11.875f, 18f, 4.875f, 18f, 4.875f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.375f, 13f)
                lineTo(7.875f, 9.5f)
                lineTo(11.375f, 6f)
            }
        }.build()

        return _UndoAction!!
    }

@Suppress("ObjectPropertyName")
private var _UndoAction: ImageVector? = null
