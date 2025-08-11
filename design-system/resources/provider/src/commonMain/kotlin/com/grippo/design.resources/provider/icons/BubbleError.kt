package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BubbleError: ImageVector
    get() {
        if (_BubbleError != null) {
            return _BubbleError!!
        }
        _BubbleError = ImageVector.Builder(
            name = "BubbleError",
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
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.242f, 5.243f)
                lineTo(21.364f, 7.364f)
                moveTo(17.121f, 7.364f)
                lineTo(19.242f, 5.243f)
                lineTo(17.121f, 7.364f)
                close()
                moveTo(19.242f, 5.243f)
                lineTo(21.364f, 3.121f)
                lineTo(19.242f, 5.243f)
                close()
                moveTo(19.242f, 5.243f)
                lineTo(17.121f, 3.121f)
                lineTo(19.242f, 5.243f)
                close()
            }
        }.build()

        return _BubbleError!!
    }

@Suppress("ObjectPropertyName")
private var _BubbleError: ImageVector? = null
