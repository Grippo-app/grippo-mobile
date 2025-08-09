package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Fog: ImageVector
    get() {
        if (_Fog != null) {
            return _Fog!!
        }
        _Fog = ImageVector.Builder(
            name = "Fog",
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
                moveTo(9f, 14f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 22f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 18f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 17.382f)
                curveTo(2.188f, 16.707f, 1f, 15.388f, 1f, 13f)
                curveTo(1f, 9f, 4.333f, 8f, 6f, 8f)
                curveTo(6f, 6f, 6f, 2f, 12f, 2f)
                curveTo(18f, 2f, 18f, 6f, 18f, 8f)
                curveTo(19.667f, 8f, 23f, 9f, 23f, 13f)
                curveTo(23f, 15.388f, 21.812f, 16.707f, 20.5f, 17.382f)
            }
        }.build()

        return _Fog!!
    }

@Suppress("ObjectPropertyName")
private var _Fog: ImageVector? = null
