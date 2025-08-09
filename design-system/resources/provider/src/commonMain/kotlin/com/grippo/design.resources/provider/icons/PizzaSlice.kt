package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PizzaSlice: ImageVector
    get() {
        if (_PizzaSlice != null) {
            return _PizzaSlice!!
        }
        _PizzaSlice = ImageVector.Builder(
            name = "PizzaSlice",
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
                moveTo(14f, 9.01f)
                lineTo(14.01f, 8.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 8.01f)
                lineTo(8.01f, 7.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 14.01f)
                lineTo(8.01f, 13.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6f, 19f)
                lineTo(2.236f, 3.004f)
                curveTo(2.131f, 2.556f, 2.548f, 2.161f, 2.99f, 2.291f)
                lineTo(19f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(22.198f, 8.425f)
                curveTo(22.432f, 7.487f, 21.862f, 6.537f, 20.925f, 6.302f)
                curveTo(19.987f, 6.068f, 19.037f, 6.638f, 18.802f, 7.576f)
                curveTo(18.41f, 9.143f, 16.901f, 11.624f, 14.575f, 13.95f)
                curveTo(12.274f, 16.251f, 9.427f, 18.144f, 6.607f, 18.795f)
                curveTo(5.665f, 19.012f, 5.078f, 19.952f, 5.295f, 20.894f)
                curveTo(5.512f, 21.835f, 6.452f, 22.423f, 7.394f, 22.205f)
                curveTo(11.073f, 21.356f, 14.476f, 18.999f, 17.05f, 16.425f)
                curveTo(19.599f, 13.876f, 21.59f, 10.857f, 22.198f, 8.425f)
                close()
            }
        }.build()

        return _PizzaSlice!!
    }

@Suppress("ObjectPropertyName")
private var _PizzaSlice: ImageVector? = null
