package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EaseOutControlPoint: ImageVector
    get() {
        if (_EaseOutControlPoint != null) {
            return _EaseOutControlPoint!!
        }
        _EaseOutControlPoint = ImageVector.Builder(
            name = "EaseOutControlPoint",
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
                moveTo(3f, 20f)
                curveTo(3f, 20f, 13f, 4f, 21f, 4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 4f)
                horizontalLineTo(9f)
                moveTo(7f, 4f)
                curveTo(7f, 5.105f, 6.105f, 6f, 5f, 6f)
                curveTo(3.895f, 6f, 3f, 5.105f, 3f, 4f)
                curveTo(3f, 2.895f, 3.895f, 2f, 5f, 2f)
                curveTo(6.105f, 2f, 7f, 2.895f, 7f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 4f)
                horizontalLineTo(12f)
            }
        }.build()

        return _EaseOutControlPoint!!
    }

@Suppress("ObjectPropertyName")
private var _EaseOutControlPoint: ImageVector? = null
