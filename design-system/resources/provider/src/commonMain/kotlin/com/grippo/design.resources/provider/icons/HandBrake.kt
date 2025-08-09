package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.HandBrake: ImageVector
    get() {
        if (_HandBrake != null) {
            return _HandBrake!!
        }
        _HandBrake = ImageVector.Builder(
            name = "HandBrake",
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
                moveTo(12f, 16f)
                verticalLineTo(14f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(8.5f)
                verticalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 20f)
                curveTo(16.418f, 20f, 20f, 16.418f, 20f, 12f)
                curveTo(20f, 7.582f, 16.418f, 4f, 12f, 4f)
                curveTo(7.582f, 4f, 4f, 7.582f, 4f, 12f)
                curveTo(4f, 16.418f, 7.582f, 20f, 12f, 20f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.953f, 4.5f)
                curveTo(2.121f, 6.465f, 1f, 9.102f, 1f, 12f)
                curveTo(1f, 14.899f, 2.121f, 17.535f, 3.953f, 19.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.047f, 4.5f)
                curveTo(21.879f, 6.465f, 23f, 9.102f, 23f, 12f)
                curveTo(23f, 14.899f, 21.879f, 17.535f, 20.047f, 19.5f)
            }
        }.build()

        return _HandBrake!!
    }

@Suppress("ObjectPropertyName")
private var _HandBrake: ImageVector? = null
