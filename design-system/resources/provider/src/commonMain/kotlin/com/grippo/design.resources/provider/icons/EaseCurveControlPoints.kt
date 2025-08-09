package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EaseCurveControlPoints: ImageVector
    get() {
        if (_EaseCurveControlPoints != null) {
            return _EaseCurveControlPoints!!
        }
        _EaseCurveControlPoints = ImageVector.Builder(
            name = "EaseCurveControlPoints",
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
                moveTo(17f, 20f)
                horizontalLineTo(15f)
                moveTo(17f, 20f)
                curveTo(17f, 21.105f, 17.895f, 22f, 19f, 22f)
                curveTo(20.105f, 22f, 21f, 21.105f, 21f, 20f)
                curveTo(21f, 18.895f, 20.105f, 18f, 19f, 18f)
                curveTo(17.895f, 18f, 17f, 18.895f, 17f, 20f)
                close()
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
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20f)
                horizontalLineTo(10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 20f)
                curveTo(11f, 20f, 13f, 4f, 21f, 4f)
            }
        }.build()

        return _EaseCurveControlPoints!!
    }

@Suppress("ObjectPropertyName")
private var _EaseCurveControlPoints: ImageVector? = null
