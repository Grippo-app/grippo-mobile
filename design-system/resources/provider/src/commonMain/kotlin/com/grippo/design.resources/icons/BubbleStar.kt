package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BubbleStar: ImageVector
    get() {
        if (_BubbleStar != null) {
            return _BubbleStar!!
        }
        _BubbleStar = ImageVector.Builder(
            name = "BubbleStar",
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
                moveTo(17.306f, 4.113f)
                lineTo(18.215f, 2.186f)
                curveTo(18.331f, 1.938f, 18.669f, 1.938f, 18.785f, 2.186f)
                lineTo(19.694f, 4.113f)
                lineTo(21.728f, 4.424f)
                curveTo(21.988f, 4.464f, 22.092f, 4.8f, 21.903f, 4.992f)
                lineTo(20.433f, 6.492f)
                lineTo(20.78f, 8.61f)
                curveTo(20.824f, 8.882f, 20.552f, 9.089f, 20.318f, 8.961f)
                lineTo(18.5f, 7.96f)
                lineTo(16.682f, 8.961f)
                curveTo(16.448f, 9.089f, 16.176f, 8.882f, 16.22f, 8.61f)
                lineTo(16.567f, 6.492f)
                lineTo(15.097f, 4.992f)
                curveTo(14.908f, 4.8f, 15.012f, 4.464f, 15.273f, 4.424f)
                lineTo(17.306f, 4.113f)
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

        return _BubbleStar!!
    }

@Suppress("ObjectPropertyName")
private var _BubbleStar: ImageVector? = null
