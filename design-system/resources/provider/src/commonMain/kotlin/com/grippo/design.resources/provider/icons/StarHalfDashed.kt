package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.StarHalfDashed: ImageVector
    get() {
        if (_StarHalfDashed != null) {
            return _StarHalfDashed!!
        }
        _StarHalfDashed = ImageVector.Builder(
            name = "StarHalfDashed",
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
                moveTo(12.815f, 3.004f)
                curveTo(12.482f, 2.332f, 11.519f, 2.332f, 11.185f, 3.004f)
                lineTo(10.689f, 4.002f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18.678f)
                lineTo(10.428f, 19.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.671f, 19.369f)
                lineTo(5.486f, 20.441f)
                curveTo(5.359f, 21.18f, 6.139f, 21.743f, 6.805f, 21.394f)
                lineTo(7.653f, 20.951f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.253f, 16f)
                lineTo(6.478f, 14.692f)
                lineTo(5.783f, 14.019f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.699f, 12f)
                lineTo(2.276f, 10.622f)
                curveTo(1.736f, 10.099f, 2.033f, 9.188f, 2.779f, 9.08f)
                lineTo(3.889f, 8.919f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 8.467f)
                lineTo(8.587f, 8.236f)
                lineTo(9.391f, 6.618f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.413f, 8.236f)
                lineTo(12.815f, 3.004f)
                curveTo(12.648f, 2.668f, 12.324f, 2.5f, 12f, 2.5f)
                verticalLineTo(18.678f)
                lineTo(17.194f, 21.394f)
                curveTo(17.861f, 21.743f, 18.641f, 21.18f, 18.514f, 20.441f)
                lineTo(17.522f, 14.692f)
                lineTo(21.724f, 10.622f)
                curveTo(22.264f, 10.099f, 21.967f, 9.188f, 21.222f, 9.08f)
                lineTo(15.413f, 8.236f)
                close()
            }
        }.build()

        return _StarHalfDashed!!
    }

@Suppress("ObjectPropertyName")
private var _StarHalfDashed: ImageVector? = null
