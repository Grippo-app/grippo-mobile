package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Yoga: ImageVector
    get() {
        if (_Yoga != null) {
            return _Yoga!!
        }
        _Yoga = ImageVector.Builder(
            name = "Yoga",
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
                moveTo(14.571f, 15.004f)
                lineTo(15.429f, 16.849f)
                curveTo(15.429f, 16.849f, 19.286f, 17.668f, 19.286f, 19.616f)
                curveTo(19.286f, 21f, 17.571f, 21f, 17.571f, 21f)
                horizontalLineTo(13f)
                lineTo(10.75f, 19.75f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.429f, 15.004f)
                lineTo(8.571f, 16.849f)
                curveTo(8.571f, 16.849f, 4.714f, 17.668f, 4.714f, 19.616f)
                curveTo(4.714f, 21f, 6.429f, 21f, 6.429f, 21f)
                horizontalLineTo(8.5f)
                lineTo(10.75f, 19.75f)
                lineTo(13.5f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 15.926f)
                curveTo(3f, 15.926f, 5.143f, 15.465f, 6.429f, 15.004f)
                curveTo(7.714f, 8.546f, 11.571f, 9.007f, 12f, 9.007f)
                curveTo(12.429f, 9.007f, 16.286f, 8.546f, 17.571f, 15.004f)
                curveTo(18.857f, 15.465f, 21f, 15.926f, 21f, 15.926f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 7f)
                curveTo(13.105f, 7f, 14f, 6.105f, 14f, 5f)
                curveTo(14f, 3.895f, 13.105f, 3f, 12f, 3f)
                curveTo(10.895f, 3f, 10f, 3.895f, 10f, 5f)
                curveTo(10f, 6.105f, 10.895f, 7f, 12f, 7f)
                close()
            }
        }.build()

        return _Yoga!!
    }

@Suppress("ObjectPropertyName")
private var _Yoga: ImageVector? = null
