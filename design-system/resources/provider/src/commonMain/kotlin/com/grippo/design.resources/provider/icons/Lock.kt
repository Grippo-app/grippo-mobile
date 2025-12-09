package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Lock: ImageVector
    get() {
        if (_Lock != null) {
            return _Lock!!
        }
        _Lock = ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(4f, 13f)
                curveTo(4f, 11.114f, 4f, 10.172f, 4.586f, 9.586f)
                curveTo(5.172f, 9f, 6.114f, 9f, 8f, 9f)
                horizontalLineTo(16f)
                curveTo(17.886f, 9f, 18.828f, 9f, 19.414f, 9.586f)
                curveTo(20f, 10.172f, 20f, 11.114f, 20f, 13f)
                verticalLineTo(15f)
                curveTo(20f, 17.828f, 20f, 19.243f, 19.121f, 20.121f)
                curveTo(18.243f, 21f, 16.828f, 21f, 14f, 21f)
                horizontalLineTo(10f)
                curveTo(7.172f, 21f, 5.757f, 21f, 4.879f, 20.121f)
                curveTo(4f, 19.243f, 4f, 17.828f, 4f, 15f)
                verticalLineTo(13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 8f)
                verticalLineTo(7f)
                curveTo(16f, 4.791f, 14.209f, 3f, 12f, 3f)
                curveTo(9.791f, 3f, 8f, 4.791f, 8f, 7f)
                verticalLineTo(8f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 15f)
                moveToRelative(-2f, 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
            }
        }.build()

        return _Lock!!
    }

@Suppress("ObjectPropertyName")
private var _Lock: ImageVector? = null
