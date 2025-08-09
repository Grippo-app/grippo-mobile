package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Infinite: ImageVector
    get() {
        if (_Infinite != null) {
            return _Infinite!!
        }
        _Infinite = ImageVector.Builder(
            name = "Infinite",
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
                moveTo(10f, 9f)
                curveTo(9.088f, 7.786f, 7.636f, 7f, 6f, 7f)
                curveTo(3.239f, 7f, 1f, 9.239f, 1f, 12f)
                curveTo(1f, 14.761f, 3.239f, 17f, 6f, 17f)
                curveTo(7.636f, 17f, 9.088f, 16.214f, 10f, 15f)
                lineTo(10.334f, 14.5f)
                moveTo(14f, 9f)
                lineTo(13.75f, 9.375f)
                lineTo(14f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 9f)
                lineTo(14f, 15f)
                curveTo(14.912f, 16.214f, 16.364f, 17f, 18f, 17f)
                curveTo(20.761f, 17f, 23f, 14.761f, 23f, 12f)
                curveTo(23f, 9.239f, 20.761f, 7f, 18f, 7f)
                curveTo(16.364f, 7f, 14.912f, 7.786f, 14f, 9f)
            }
        }.build()

        return _Infinite!!
    }

@Suppress("ObjectPropertyName")
private var _Infinite: ImageVector? = null
