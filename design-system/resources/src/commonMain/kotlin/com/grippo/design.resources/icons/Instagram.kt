package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Instagram: ImageVector
    get() {
        if (_Instagram != null) {
            return _Instagram!!
        }
        _Instagram = ImageVector.Builder(
            name = "Instagram",
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
                horizontalLineTo(7f)
                curveTo(4.239f, 2f, 2f, 4.239f, 2f, 7f)
                verticalLineTo(17f)
                curveTo(2f, 19.761f, 4.239f, 22f, 7f, 22f)
                horizontalLineTo(17f)
                curveTo(19.761f, 22f, 22f, 19.761f, 22f, 17f)
                verticalLineTo(7f)
                curveTo(22f, 4.239f, 19.761f, 2f, 17f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 11.37f)
                curveTo(16.123f, 12.202f, 15.981f, 13.052f, 15.594f, 13.799f)
                curveTo(15.206f, 14.546f, 14.593f, 15.151f, 13.842f, 15.53f)
                curveTo(13.09f, 15.908f, 12.238f, 16.04f, 11.408f, 15.906f)
                curveTo(10.577f, 15.772f, 9.81f, 15.38f, 9.215f, 14.785f)
                curveTo(8.62f, 14.19f, 8.228f, 13.423f, 8.094f, 12.592f)
                curveTo(7.96f, 11.762f, 8.092f, 10.91f, 8.47f, 10.158f)
                curveTo(8.849f, 9.407f, 9.454f, 8.794f, 10.201f, 8.406f)
                curveTo(10.948f, 8.019f, 11.798f, 7.877f, 12.63f, 8f)
                curveTo(13.479f, 8.126f, 14.265f, 8.521f, 14.872f, 9.128f)
                curveTo(15.479f, 9.735f, 15.874f, 10.521f, 16f, 11.37f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 6.5f)
                horizontalLineTo(17.51f)
            }
        }.build()

        return _Instagram!!
    }

@Suppress("ObjectPropertyName")
private var _Instagram: ImageVector? = null
