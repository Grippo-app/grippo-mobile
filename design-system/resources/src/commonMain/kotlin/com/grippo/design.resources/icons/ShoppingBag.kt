package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShoppingBag: ImageVector
    get() {
        if (_ShoppingBag != null) {
            return _ShoppingBag!!
        }
        _ShoppingBag = ImageVector.Builder(
            name = "ShoppingBag",
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
                moveTo(6f, 2f)
                lineTo(3f, 6f)
                verticalLineTo(20f)
                curveTo(3f, 20.53f, 3.211f, 21.039f, 3.586f, 21.414f)
                curveTo(3.961f, 21.789f, 4.47f, 22f, 5f, 22f)
                horizontalLineTo(19f)
                curveTo(19.53f, 22f, 20.039f, 21.789f, 20.414f, 21.414f)
                curveTo(20.789f, 21.039f, 21f, 20.53f, 21f, 20f)
                verticalLineTo(6f)
                lineTo(18f, 2f)
                horizontalLineTo(6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 10f)
                curveTo(16f, 11.061f, 15.579f, 12.078f, 14.828f, 12.828f)
                curveTo(14.078f, 13.579f, 13.061f, 14f, 12f, 14f)
                curveTo(10.939f, 14f, 9.922f, 13.579f, 9.172f, 12.828f)
                curveTo(8.421f, 12.078f, 8f, 11.061f, 8f, 10f)
            }
        }.build()

        return _ShoppingBag!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingBag: ImageVector? = null
