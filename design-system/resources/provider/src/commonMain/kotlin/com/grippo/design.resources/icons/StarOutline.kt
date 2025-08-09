package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.StarOutline: ImageVector
    get() {
        if (_StarOutline != null) {
            return _StarOutline!!
        }
        _StarOutline = ImageVector.Builder(
            name = "StarOutline",
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
                moveTo(8.587f, 8.236f)
                lineTo(11.185f, 3.004f)
                curveTo(11.518f, 2.332f, 12.482f, 2.332f, 12.815f, 3.004f)
                lineTo(15.413f, 8.236f)
                lineTo(21.222f, 9.08f)
                curveTo(21.967f, 9.188f, 22.264f, 10.099f, 21.724f, 10.622f)
                lineTo(17.522f, 14.692f)
                lineTo(18.514f, 20.441f)
                curveTo(18.641f, 21.18f, 17.861f, 21.743f, 17.194f, 21.394f)
                lineTo(12f, 18.678f)
                lineTo(6.805f, 21.394f)
                curveTo(6.139f, 21.743f, 5.359f, 21.18f, 5.486f, 20.441f)
                lineTo(6.478f, 14.692f)
                lineTo(2.276f, 10.622f)
                curveTo(1.736f, 10.099f, 2.033f, 9.188f, 2.779f, 9.08f)
                lineTo(8.587f, 8.236f)
                close()
            }
        }.build()

        return _StarOutline!!
    }

@Suppress("ObjectPropertyName")
private var _StarOutline: ImageVector? = null
