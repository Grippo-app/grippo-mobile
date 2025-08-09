package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Potion: ImageVector
    get() {
        if (_Potion != null) {
            return _Potion!!
        }
        _Potion = ImageVector.Builder(
            name = "Potion",
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
                moveTo(10f, 4f)
                lineTo(14f, 4f)
                verticalLineTo(6.568f)
                curveTo(14f, 6.826f, 14.171f, 7.055f, 14.413f, 7.147f)
                curveTo(22.937f, 10.371f, 20.907f, 22f, 15f, 22f)
                horizontalLineTo(9f)
                curveTo(3.094f, 22f, 1.063f, 10.371f, 9.588f, 7.147f)
                curveTo(9.829f, 7.055f, 10f, 6.826f, 10f, 6.568f)
                verticalLineTo(4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6f, 10f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 2f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.667f, 13f)
                lineTo(10f, 16f)
                horizontalLineTo(14f)
                lineTo(12.333f, 19f)
            }
        }.build()

        return _Potion!!
    }

@Suppress("ObjectPropertyName")
private var _Potion: ImageVector? = null
