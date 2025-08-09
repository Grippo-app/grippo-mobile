package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EvChargeAlt: ImageVector
    get() {
        if (_EvChargeAlt != null) {
            return _EvChargeAlt!!
        }
        _EvChargeAlt = ImageVector.Builder(
            name = "EvChargeAlt",
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
                moveTo(22f, 5f)
                lineTo(20f, 9f)
                lineTo(18f, 5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5f)
                horizontalLineTo(14f)
                verticalLineTo(9f)
                horizontalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 7f)
                horizontalLineTo(15.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 9f)
                verticalLineTo(19f)
                curveTo(6f, 20.105f, 6.895f, 21f, 8f, 21f)
                horizontalLineTo(17f)
                curveTo(18.105f, 21f, 19f, 20.105f, 19f, 19f)
                verticalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 5.6f)
                verticalLineTo(7f)
                curveTo(9f, 8.105f, 8.105f, 9f, 7f, 9f)
                horizontalLineTo(5f)
                curveTo(3.895f, 9f, 3f, 8.105f, 3f, 7f)
                verticalLineTo(5.6f)
                curveTo(3f, 5.269f, 3.269f, 5f, 3.6f, 5f)
                horizontalLineTo(8.4f)
                curveTo(8.731f, 5f, 9f, 5.269f, 9f, 5.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 5f)
                verticalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 5f)
                verticalLineTo(3f)
            }
        }.build()

        return _EvChargeAlt!!
    }

@Suppress("ObjectPropertyName")
private var _EvChargeAlt: ImageVector? = null
