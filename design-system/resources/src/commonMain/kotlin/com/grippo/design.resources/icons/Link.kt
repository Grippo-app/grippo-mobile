package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Link: ImageVector
    get() {
        if (_Link != null) {
            return _Link!!
        }
        _Link = ImageVector.Builder(
            name = "Link",
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
                moveTo(10f, 13f)
                curveTo(10.429f, 13.574f, 10.977f, 14.049f, 11.607f, 14.393f)
                curveTo(12.236f, 14.737f, 12.932f, 14.941f, 13.647f, 14.992f)
                curveTo(14.362f, 15.044f, 15.08f, 14.94f, 15.751f, 14.69f)
                curveTo(16.423f, 14.439f, 17.033f, 14.047f, 17.54f, 13.54f)
                lineTo(20.54f, 10.54f)
                curveTo(21.451f, 9.597f, 21.955f, 8.334f, 21.943f, 7.023f)
                curveTo(21.932f, 5.712f, 21.406f, 4.458f, 20.479f, 3.531f)
                curveTo(19.552f, 2.604f, 18.298f, 2.078f, 16.987f, 2.067f)
                curveTo(15.676f, 2.055f, 14.413f, 2.559f, 13.47f, 3.47f)
                lineTo(11.75f, 5.18f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 11f)
                curveTo(13.571f, 10.426f, 13.023f, 9.951f, 12.394f, 9.607f)
                curveTo(11.764f, 9.263f, 11.068f, 9.059f, 10.353f, 9.008f)
                curveTo(9.638f, 8.956f, 8.92f, 9.06f, 8.249f, 9.31f)
                curveTo(7.577f, 9.561f, 6.967f, 9.953f, 6.46f, 10.46f)
                lineTo(3.46f, 13.46f)
                curveTo(2.549f, 14.403f, 2.045f, 15.666f, 2.057f, 16.977f)
                curveTo(2.068f, 18.288f, 2.594f, 19.542f, 3.521f, 20.469f)
                curveTo(4.448f, 21.396f, 5.702f, 21.922f, 7.013f, 21.933f)
                curveTo(8.324f, 21.945f, 9.587f, 21.441f, 10.53f, 20.53f)
                lineTo(12.24f, 18.82f)
            }
        }.build()

        return _Link!!
    }

@Suppress("ObjectPropertyName")
private var _Link: ImageVector? = null
