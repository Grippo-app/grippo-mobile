package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.UserOutline: ImageVector
    get() {
        if (_UserOutline != null) {
            return _UserOutline!!
        }
        _UserOutline = ImageVector.Builder(
            name = "UserOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter,
            ) {
                moveTo(19.7274f, 20.4471f)
                curveTo(19.2716f, 19.1713f, 18.2672f, 18.0439f, 16.8701f, 17.2399f)
                curveTo(15.4729f, 16.4358f, 13.7611f, 16f, 12f, 16f)
                curveTo(10.2389f, 16f, 8.52706f, 16.4358f, 7.12991f, 17.2399f)
                curveTo(5.73276f, 18.0439f, 4.72839f, 19.1713f, 4.27259f, 20.4471f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter,
            ) {
                moveTo(8f, 8f)
                arcToRelative(4f, 4f, 0f, true, false, 8f, 0f)
                arcToRelative(4f, 4f, 0f, true, false, -8f, 0f)
            }
        }.build()

        return _UserOutline!!
    }

@Suppress("ObjectPropertyName")
private var _UserOutline: ImageVector? = null
