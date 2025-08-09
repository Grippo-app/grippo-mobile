package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Restart: ImageVector
    get() {
        if (_Restart != null) {
            return _Restart!!
        }
        _Restart = ImageVector.Builder(
            name = "Restart",
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
                moveTo(6.677f, 20.567f)
                curveTo(2.531f, 18.021f, 0.758f, 12.758f, 2.717f, 8.144f)
                curveTo(4.875f, 3.06f, 10.745f, 0.688f, 15.829f, 2.846f)
                curveTo(20.913f, 5.004f, 23.285f, 10.875f, 21.127f, 15.958f)
                curveTo(20.284f, 17.945f, 18.874f, 19.517f, 17.165f, 20.567f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 16f)
                verticalLineTo(20.4f)
                curveTo(17f, 20.731f, 17.269f, 21f, 17.6f, 21f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22.01f)
                lineTo(12.01f, 21.999f)
            }
        }.build()

        return _Restart!!
    }

@Suppress("ObjectPropertyName")
private var _Restart: ImageVector? = null
