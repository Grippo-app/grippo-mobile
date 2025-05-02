package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RefreshCw: ImageVector
    get() {
        if (_RefreshCw != null) {
            return _RefreshCw!!
        }
        _RefreshCw = ImageVector.Builder(
            name = "RefreshCw",
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
                moveTo(23f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 14f)
                lineTo(5.64f, 18.36f)
                curveTo(6.715f, 19.435f, 8.044f, 20.221f, 9.505f, 20.643f)
                curveTo(10.965f, 21.066f, 12.509f, 21.111f, 13.992f, 20.776f)
                curveTo(15.474f, 20.44f, 16.848f, 19.735f, 17.985f, 18.725f)
                curveTo(19.121f, 17.715f, 19.983f, 16.433f, 20.49f, 15f)
                moveTo(3.51f, 9f)
                curveTo(4.017f, 7.567f, 4.879f, 6.285f, 6.015f, 5.275f)
                curveTo(7.152f, 4.265f, 8.525f, 3.56f, 10.008f, 3.224f)
                curveTo(11.491f, 2.889f, 13.035f, 2.934f, 14.495f, 3.357f)
                curveTo(15.956f, 3.779f, 17.285f, 4.565f, 18.36f, 5.64f)
                lineTo(23f, 10f)
                lineTo(3.51f, 9f)
                close()
            }
        }.build()

        return _RefreshCw!!
    }

@Suppress("ObjectPropertyName")
private var _RefreshCw: ImageVector? = null
