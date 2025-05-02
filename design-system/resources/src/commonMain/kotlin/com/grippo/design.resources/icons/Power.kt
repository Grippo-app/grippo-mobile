package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Power: ImageVector
    get() {
        if (_Power != null) {
            return _Power!!
        }
        _Power = ImageVector.Builder(
            name = "Power",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.36f, 6.64f)
                curveTo(19.618f, 7.899f, 20.475f, 9.502f, 20.822f, 11.248f)
                curveTo(21.169f, 12.994f, 20.991f, 14.803f, 20.31f, 16.448f)
                curveTo(19.628f, 18.092f, 18.475f, 19.498f, 16.995f, 20.486f)
                curveTo(15.515f, 21.475f, 13.775f, 22.003f, 11.995f, 22.003f)
                curveTo(10.215f, 22.003f, 8.475f, 21.475f, 6.995f, 20.486f)
                curveTo(5.515f, 19.498f, 4.362f, 18.092f, 3.68f, 16.448f)
                curveTo(2.999f, 14.803f, 2.821f, 12.994f, 3.168f, 11.248f)
                curveTo(3.515f, 9.502f, 4.372f, 7.899f, 5.63f, 6.64f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                verticalLineTo(12f)
            }
        }.build()

        return _Power!!
    }

@Suppress("ObjectPropertyName")
private var _Power: ImageVector? = null
