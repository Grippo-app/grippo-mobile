package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ThumbsDown: ImageVector
    get() {
        if (_ThumbsDown != null) {
            return _ThumbsDown!!
        }
        _ThumbsDown = ImageVector.Builder(
            name = "ThumbsDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.472f, 3.5f)
                horizontalLineTo(4.1f)
                curveTo(3.769f, 3.5f, 3.5f, 3.769f, 3.5f, 4.1f)
                verticalLineTo(13.9f)
                curveTo(3.5f, 14.231f, 3.769f, 14.5f, 4.1f, 14.5f)
                horizontalLineTo(6.868f)
                curveTo(7.57f, 14.5f, 8.221f, 14.869f, 8.583f, 15.471f)
                lineTo(11.293f, 19.988f)
                curveTo(11.878f, 20.963f, 13.255f, 21.056f, 13.965f, 20.168f)
                curveTo(14.3f, 19.75f, 14.408f, 19.194f, 14.254f, 18.68f)
                lineTo(13.232f, 15.272f)
                curveTo(13.116f, 14.887f, 13.405f, 14.5f, 13.806f, 14.5f)
                horizontalLineTo(18.382f)
                curveTo(19.7f, 14.5f, 20.658f, 13.246f, 20.311f, 11.974f)
                lineTo(18.402f, 4.974f)
                curveTo(18.165f, 4.104f, 17.374f, 3.5f, 16.472f, 3.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 14.5f)
                verticalLineTo(3.5f)
            }
        }.build()

        return _ThumbsDown!!
    }

@Suppress("ObjectPropertyName")
private var _ThumbsDown: ImageVector? = null
