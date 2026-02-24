package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ChatAlt: ImageVector
    get() {
        if (_ChatAlt != null) {
            return _ChatAlt!!
        }
        _ChatAlt = ImageVector.Builder(
            name = "ChatAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(20f, 12f)
                curveTo(20f, 8.229f, 20f, 6.343f, 18.828f, 5.172f)
                curveTo(17.657f, 4f, 15.771f, 4f, 12f, 4f)
                curveTo(8.229f, 4f, 6.343f, 4f, 5.172f, 5.172f)
                curveTo(4f, 6.343f, 4f, 8.229f, 4f, 12f)
                verticalLineTo(18f)
                curveTo(4f, 18.943f, 4f, 19.414f, 4.293f, 19.707f)
                curveTo(4.586f, 20f, 5.057f, 20f, 6f, 20f)
                horizontalLineTo(12f)
                curveTo(15.771f, 20f, 17.657f, 20f, 18.828f, 18.828f)
                curveTo(20f, 17.657f, 20f, 15.771f, 20f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 10f)
                lineTo(15f, 10f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 14f)
                horizontalLineTo(12f)
            }
        }.build()

        return _ChatAlt!!
    }

@Suppress("ObjectPropertyName")
private var _ChatAlt: ImageVector? = null
