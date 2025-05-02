package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Users: ImageVector
    get() {
        if (_Users != null) {
            return _Users!!
        }
        _Users = ImageVector.Builder(
            name = "Users",
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
                moveTo(17f, 21f)
                verticalLineTo(19f)
                curveTo(17f, 17.939f, 16.579f, 16.922f, 15.828f, 16.172f)
                curveTo(15.078f, 15.421f, 14.061f, 15f, 13f, 15f)
                horizontalLineTo(5f)
                curveTo(3.939f, 15f, 2.922f, 15.421f, 2.172f, 16.172f)
                curveTo(1.421f, 16.922f, 1f, 17.939f, 1f, 19f)
                verticalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 11f)
                curveTo(11.209f, 11f, 13f, 9.209f, 13f, 7f)
                curveTo(13f, 4.791f, 11.209f, 3f, 9f, 3f)
                curveTo(6.791f, 3f, 5f, 4.791f, 5f, 7f)
                curveTo(5f, 9.209f, 6.791f, 11f, 9f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 21f)
                verticalLineTo(19f)
                curveTo(22.999f, 18.114f, 22.704f, 17.253f, 22.161f, 16.552f)
                curveTo(21.618f, 15.852f, 20.858f, 15.352f, 20f, 15.13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 3.13f)
                curveTo(16.86f, 3.35f, 17.623f, 3.851f, 18.168f, 4.552f)
                curveTo(18.712f, 5.254f, 19.008f, 6.117f, 19.008f, 7.005f)
                curveTo(19.008f, 7.893f, 18.712f, 8.756f, 18.168f, 9.458f)
                curveTo(17.623f, 10.159f, 16.86f, 10.66f, 16f, 10.88f)
            }
        }.build()

        return _Users!!
    }

@Suppress("ObjectPropertyName")
private var _Users: ImageVector? = null
