package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RefreshCcw: ImageVector
    get() {
        if (_RefreshCcw != null) {
            return _RefreshCcw!!
        }
        _RefreshCcw = ImageVector.Builder(
            name = "RefreshCcw",
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
                moveTo(1f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 14f)
                lineTo(18.36f, 18.36f)
                curveTo(17.285f, 19.435f, 15.956f, 20.221f, 14.495f, 20.643f)
                curveTo(13.035f, 21.066f, 11.491f, 21.111f, 10.008f, 20.776f)
                curveTo(8.525f, 20.44f, 7.152f, 19.735f, 6.015f, 18.725f)
                curveTo(4.879f, 17.715f, 4.017f, 16.433f, 3.51f, 15f)
                moveTo(20.49f, 9f)
                curveTo(19.983f, 7.567f, 19.121f, 6.285f, 17.985f, 5.275f)
                curveTo(16.848f, 4.265f, 15.474f, 3.56f, 13.992f, 3.224f)
                curveTo(12.509f, 2.889f, 10.965f, 2.934f, 9.505f, 3.357f)
                curveTo(8.044f, 3.779f, 6.715f, 4.565f, 5.64f, 5.64f)
                lineTo(1f, 10f)
                lineTo(20.49f, 9f)
                close()
            }
        }.build()

        return _RefreshCcw!!
    }

@Suppress("ObjectPropertyName")
private var _RefreshCcw: ImageVector? = null
