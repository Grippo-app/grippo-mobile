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
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 2f)
                horizontalLineTo(19.67f)
                curveTo(20.236f, 1.99f, 20.786f, 2.188f, 21.215f, 2.557f)
                curveTo(21.645f, 2.925f, 21.924f, 3.439f, 22f, 4f)
                verticalLineTo(11f)
                curveTo(21.924f, 11.561f, 21.645f, 12.075f, 21.215f, 12.443f)
                curveTo(20.786f, 12.812f, 20.236f, 13.01f, 19.67f, 13f)
                horizontalLineTo(17f)
                moveTo(10f, 15f)
                verticalLineTo(19f)
                curveTo(10f, 19.796f, 10.316f, 20.559f, 10.879f, 21.121f)
                curveTo(11.441f, 21.684f, 12.204f, 22f, 13f, 22f)
                lineTo(17f, 13f)
                verticalLineTo(2f)
                horizontalLineTo(5.72f)
                curveTo(5.238f, 1.995f, 4.77f, 2.164f, 4.402f, 2.476f)
                curveTo(4.035f, 2.788f, 3.792f, 3.223f, 3.72f, 3.7f)
                lineTo(2.34f, 12.7f)
                curveTo(2.297f, 12.987f, 2.316f, 13.279f, 2.397f, 13.558f)
                curveTo(2.478f, 13.836f, 2.618f, 14.094f, 2.808f, 14.313f)
                curveTo(2.998f, 14.531f, 3.234f, 14.706f, 3.498f, 14.825f)
                curveTo(3.763f, 14.943f, 4.05f, 15.003f, 4.34f, 15f)
                horizontalLineTo(10f)
                close()
            }
        }.build()

        return _ThumbsDown!!
    }

@Suppress("ObjectPropertyName")
private var _ThumbsDown: ImageVector? = null
