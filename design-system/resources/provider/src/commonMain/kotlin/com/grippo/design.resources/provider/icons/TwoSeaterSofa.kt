package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.TwoSeaterSofa: ImageVector
    get() {
        if (_TwoSeaterSofa != null) {
            return _TwoSeaterSofa!!
        }
        _TwoSeaterSofa = ImageVector.Builder(
            name = "TwoSeaterSofa",
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
                moveTo(2f, 16f)
                verticalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 13f)
                verticalLineTo(7f)
                curveTo(12f, 5.895f, 12.895f, 5f, 14f, 5f)
                horizontalLineTo(18f)
                curveTo(19.105f, 5f, 20f, 5.895f, 20f, 7f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 13f)
                verticalLineTo(7f)
                curveTo(12f, 5.895f, 11.105f, 5f, 10f, 5f)
                horizontalLineTo(6f)
                curveTo(4.895f, 5f, 4f, 5.895f, 4f, 7f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 9f)
                curveTo(18.895f, 9f, 18f, 9.895f, 18f, 11f)
                verticalLineTo(13f)
                horizontalLineTo(6f)
                verticalLineTo(11f)
                curveTo(6f, 9.895f, 5.105f, 9f, 4f, 9f)
                curveTo(2.895f, 9f, 2f, 9.895f, 2f, 11f)
                verticalLineTo(17f)
                horizontalLineTo(22f)
                verticalLineTo(11f)
                curveTo(22f, 9.895f, 21.105f, 9f, 20f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 16f)
                verticalLineTo(19f)
            }
        }.build()

        return _TwoSeaterSofa!!
    }

@Suppress("ObjectPropertyName")
private var _TwoSeaterSofa: ImageVector? = null
