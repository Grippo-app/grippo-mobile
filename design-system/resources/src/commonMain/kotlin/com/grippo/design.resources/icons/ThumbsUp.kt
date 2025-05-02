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
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 22f)
                horizontalLineTo(4f)
                curveTo(3.47f, 22f, 2.961f, 21.789f, 2.586f, 21.414f)
                curveTo(2.211f, 21.039f, 2f, 20.53f, 2f, 20f)
                verticalLineTo(13f)
                curveTo(2f, 12.47f, 2.211f, 11.961f, 2.586f, 11.586f)
                curveTo(2.961f, 11.211f, 3.47f, 11f, 4f, 11f)
                horizontalLineTo(7f)
                moveTo(14f, 9f)
                verticalLineTo(5f)
                curveTo(14f, 4.204f, 13.684f, 3.441f, 13.121f, 2.879f)
                curveTo(12.559f, 2.316f, 11.796f, 2f, 11f, 2f)
                lineTo(7f, 11f)
                verticalLineTo(22f)
                horizontalLineTo(18.28f)
                curveTo(18.762f, 22.006f, 19.23f, 21.836f, 19.598f, 21.524f)
                curveTo(19.965f, 21.212f, 20.208f, 20.777f, 20.28f, 20.3f)
                lineTo(21.66f, 11.3f)
                curveTo(21.704f, 11.013f, 21.684f, 10.721f, 21.603f, 10.442f)
                curveTo(21.522f, 10.164f, 21.382f, 9.906f, 21.192f, 9.688f)
                curveTo(21.002f, 9.469f, 20.766f, 9.294f, 20.502f, 9.175f)
                curveTo(20.237f, 9.057f, 19.95f, 8.997f, 19.66f, 9f)
                horizontalLineTo(14f)
                close()
            }
        }.build()

        return _ThumbsUp!!
    }

@Suppress("ObjectPropertyName")
private var _ThumbsUp: ImageVector? = null
