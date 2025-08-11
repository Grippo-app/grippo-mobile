package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BubbleSearch: ImageVector
    get() {
        if (_BubbleSearch != null) {
            return _BubbleSearch!!
        }
        _BubbleSearch = ImageVector.Builder(
            name = "BubbleSearch",
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
                moveTo(20.5f, 6.5f)
                lineTo(22f, 8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 4.5f)
                curveTo(16f, 5.881f, 17.119f, 7f, 18.5f, 7f)
                curveTo(19.192f, 7f, 19.817f, 6.719f, 20.27f, 6.265f)
                curveTo(20.721f, 5.813f, 21f, 5.189f, 21f, 4.5f)
                curveTo(21f, 3.119f, 19.881f, 2f, 18.5f, 2f)
                curveTo(17.119f, 2f, 16f, 3.119f, 16f, 4.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 2.049f)
                curveTo(12.671f, 2.017f, 12.337f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 13.821f, 2.487f, 15.529f, 3.338f, 17f)
                lineTo(2.5f, 21.5f)
                lineTo(7f, 20.662f)
                curveTo(8.471f, 21.513f, 10.179f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 11.663f, 21.983f, 11.329f, 21.951f, 11f)
            }
        }.build()

        return _BubbleSearch!!
    }

@Suppress("ObjectPropertyName")
private var _BubbleSearch: ImageVector? = null
