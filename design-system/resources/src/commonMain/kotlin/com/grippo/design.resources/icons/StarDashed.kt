package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.StarDashed: ImageVector
    get() {
        if (_StarDashed != null) {
            return _StarDashed!!
        }
        _StarDashed = ImageVector.Builder(
            name = "StarDashed",
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
                moveTo(13.806f, 5f)
                lineTo(12.815f, 3.004f)
                curveTo(12.482f, 2.332f, 11.519f, 2.332f, 11.185f, 3.004f)
                lineTo(10.689f, 4.002f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.011f, 7.427f)
                lineTo(15.413f, 8.236f)
                lineTo(16.865f, 8.447f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.769f, 8.869f)
                lineTo(21.222f, 9.08f)
                curveTo(21.967f, 9.189f, 22.264f, 10.099f, 21.724f, 10.622f)
                lineTo(20.674f, 11.639f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.572f, 13.674f)
                lineTo(17.522f, 14.692f)
                lineTo(17.77f, 16.129f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.266f, 19.004f)
                lineTo(18.514f, 20.441f)
                curveTo(18.641f, 21.18f, 17.861f, 21.743f, 17.195f, 21.394f)
                lineTo(15.896f, 20.715f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.428f, 19.5f)
                lineTo(12f, 18.678f)
                lineTo(13.299f, 19.357f)
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
        }.build()

        return _StarDashed!!
    }

@Suppress("ObjectPropertyName")
private var _StarDashed: ImageVector? = null
