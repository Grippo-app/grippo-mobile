package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Atom: ImageVector
    get() {
        if (_Atom != null) {
            return _Atom!!
        }
        _Atom = ImageVector.Builder(
            name = "Atom",
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
                moveTo(4.404f, 13.61f)
                curveTo(3.515f, 13.145f, 3f, 12.592f, 3f, 12f)
                curveTo(3f, 10.343f, 7.029f, 9f, 12f, 9f)
                curveTo(16.971f, 9f, 21f, 10.343f, 21f, 12f)
                curveTo(21f, 12.714f, 20.251f, 13.37f, 19f, 13.886f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11.01f)
                lineTo(12.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.883f, 6f)
                curveTo(16.878f, 4.977f, 16.62f, 4.253f, 16.086f, 3.981f)
                curveTo(14.609f, 3.229f, 11.583f, 6.209f, 9.327f, 10.638f)
                curveTo(7.07f, 15.067f, 6.437f, 19.267f, 7.914f, 20.019f)
                curveTo(8.441f, 20.288f, 9.166f, 20.08f, 9.984f, 19.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.601f, 4.252f)
                curveTo(8.941f, 3.866f, 8.357f, 3.755f, 7.914f, 3.981f)
                curveTo(6.437f, 4.733f, 7.07f, 8.933f, 9.327f, 13.362f)
                curveTo(11.583f, 17.791f, 14.609f, 20.771f, 16.086f, 20.019f)
                curveTo(17.398f, 19.35f, 17.044f, 15.958f, 15.364f, 12.102f)
            }
        }.build()

        return _Atom!!
    }

@Suppress("ObjectPropertyName")
private var _Atom: ImageVector? = null
