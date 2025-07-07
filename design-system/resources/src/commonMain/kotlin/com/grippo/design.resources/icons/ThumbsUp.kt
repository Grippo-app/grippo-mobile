package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ThumbsUp: ImageVector
    get() {
        if (_ThumbsUp != null) {
            return _ThumbsUp!!
        }
        _ThumbsUp = ImageVector.Builder(
            name = "ThumbsUp",
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
                moveTo(16.472f, 20f)
                horizontalLineTo(4.1f)
                curveTo(3.769f, 20f, 3.5f, 19.732f, 3.5f, 19.4f)
                verticalLineTo(9.6f)
                curveTo(3.5f, 9.269f, 3.769f, 9f, 4.1f, 9f)
                horizontalLineTo(6.868f)
                curveTo(7.57f, 9f, 8.221f, 8.632f, 8.583f, 8.029f)
                lineTo(11.293f, 3.512f)
                curveTo(11.878f, 2.537f, 13.255f, 2.444f, 13.965f, 3.332f)
                curveTo(14.3f, 3.75f, 14.408f, 4.306f, 14.254f, 4.82f)
                lineTo(13.232f, 8.228f)
                curveTo(13.116f, 8.613f, 13.405f, 9f, 13.806f, 9f)
                horizontalLineTo(18.382f)
                curveTo(19.7f, 9f, 20.658f, 10.254f, 20.311f, 11.526f)
                lineTo(18.402f, 18.526f)
                curveTo(18.165f, 19.396f, 17.374f, 20f, 16.472f, 20f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 20f)
                verticalLineTo(9f)
            }
        }.build()

        return _ThumbsUp!!
    }

@Suppress("ObjectPropertyName")
private var _ThumbsUp: ImageVector? = null
