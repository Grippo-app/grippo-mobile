package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Tool: ImageVector
    get() {
        if (_Tool != null) {
            return _Tool!!
        }
        _Tool = ImageVector.Builder(
            name = "Tool",
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
                moveTo(14.7f, 6.3f)
                curveTo(14.517f, 6.487f, 14.414f, 6.738f, 14.414f, 7f)
                curveTo(14.414f, 7.262f, 14.517f, 7.513f, 14.7f, 7.7f)
                lineTo(16.3f, 9.3f)
                curveTo(16.487f, 9.483f, 16.738f, 9.586f, 17f, 9.586f)
                curveTo(17.262f, 9.586f, 17.513f, 9.483f, 17.7f, 9.3f)
                lineTo(21.47f, 5.53f)
                curveTo(21.973f, 6.641f, 22.125f, 7.879f, 21.906f, 9.079f)
                curveTo(21.688f, 10.279f, 21.109f, 11.384f, 20.246f, 12.246f)
                curveTo(19.384f, 13.109f, 18.279f, 13.688f, 17.079f, 13.906f)
                curveTo(15.879f, 14.125f, 14.641f, 13.973f, 13.53f, 13.47f)
                lineTo(6.62f, 20.38f)
                curveTo(6.222f, 20.778f, 5.683f, 21.001f, 5.12f, 21.001f)
                curveTo(4.557f, 21.001f, 4.018f, 20.778f, 3.62f, 20.38f)
                curveTo(3.222f, 19.982f, 2.999f, 19.443f, 2.999f, 18.88f)
                curveTo(2.999f, 18.317f, 3.222f, 17.778f, 3.62f, 17.38f)
                lineTo(10.53f, 10.47f)
                curveTo(10.027f, 9.359f, 9.875f, 8.121f, 10.094f, 6.921f)
                curveTo(10.312f, 5.721f, 10.891f, 4.616f, 11.754f, 3.754f)
                curveTo(12.616f, 2.891f, 13.721f, 2.312f, 14.921f, 2.094f)
                curveTo(16.121f, 1.875f, 17.359f, 2.027f, 18.47f, 2.53f)
                lineTo(14.71f, 6.29f)
                lineTo(14.7f, 6.3f)
                close()
            }
        }.build()

        return _Tool!!
    }

@Suppress("ObjectPropertyName")
private var _Tool: ImageVector? = null
