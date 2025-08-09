package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Stretching: ImageVector
    get() {
        if (_Stretching != null) {
            return _Stretching!!
        }
        _Stretching = ImageVector.Builder(
            name = "Stretching",
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
                moveTo(16f, 7f)
                curveTo(17.105f, 7f, 18f, 6.105f, 18f, 5f)
                curveTo(18f, 3.895f, 17.105f, 3f, 16f, 3f)
                curveTo(14.895f, 3f, 14f, 3.895f, 14f, 5f)
                curveTo(14f, 6.105f, 14.895f, 7f, 16f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 20f)
                lineTo(9.909f, 19.476f)
                lineTo(12.636f, 14.238f)
                lineTo(13.727f, 9f)
                lineTo(8.818f, 10.048f)
                lineTo(10.455f, 12.143f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.818f, 15.286f)
                horizontalLineTo(17f)
                verticalLineTo(20f)
            }
        }.build()

        return _Stretching!!
    }

@Suppress("ObjectPropertyName")
private var _Stretching: ImageVector? = null
