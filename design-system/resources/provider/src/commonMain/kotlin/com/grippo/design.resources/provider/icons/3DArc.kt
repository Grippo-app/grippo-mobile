package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.`3DArc`: ImageVector
    get() {
        if (_3DArc != null) {
            return _3DArc!!
        }
        _3DArc = ImageVector.Builder(
            name = "3DArc",
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
                moveTo(22f, 16f)
                curveTo(22f, 10.477f, 17.523f, 6f, 12f, 6f)
                curveTo(6.477f, 6f, 2f, 10.477f, 2f, 16f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 17f)
                curveTo(2.552f, 17f, 3f, 16.552f, 3f, 16f)
                curveTo(3f, 15.448f, 2.552f, 15f, 2f, 15f)
                curveTo(1.448f, 15f, 1f, 15.448f, 1f, 16f)
                curveTo(1f, 16.552f, 1.448f, 17f, 2f, 17f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 17f)
                curveTo(22.552f, 17f, 23f, 16.552f, 23f, 16f)
                curveTo(23f, 15.448f, 22.552f, 15f, 22f, 15f)
                curveTo(21.448f, 15f, 21f, 15.448f, 21f, 16f)
                curveTo(21f, 16.552f, 21.448f, 17f, 22f, 17f)
                close()
            }
        }.build()

        return _3DArc!!
    }

@Suppress("ObjectPropertyName")
private var _3DArc: ImageVector? = null
