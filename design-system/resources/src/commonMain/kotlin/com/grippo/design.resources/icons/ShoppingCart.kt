package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShoppingCart: ImageVector
    get() {
        if (_ShoppingCart != null) {
            return _ShoppingCart!!
        }
        _ShoppingCart = ImageVector.Builder(
            name = "ShoppingCart",
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
                moveTo(9f, 22f)
                curveTo(9.552f, 22f, 10f, 21.552f, 10f, 21f)
                curveTo(10f, 20.448f, 9.552f, 20f, 9f, 20f)
                curveTo(8.448f, 20f, 8f, 20.448f, 8f, 21f)
                curveTo(8f, 21.552f, 8.448f, 22f, 9f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 22f)
                curveTo(20.552f, 22f, 21f, 21.552f, 21f, 21f)
                curveTo(21f, 20.448f, 20.552f, 20f, 20f, 20f)
                curveTo(19.448f, 20f, 19f, 20.448f, 19f, 21f)
                curveTo(19f, 21.552f, 19.448f, 22f, 20f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 1f)
                horizontalLineTo(5f)
                lineTo(7.68f, 14.39f)
                curveTo(7.771f, 14.85f, 8.022f, 15.264f, 8.388f, 15.558f)
                curveTo(8.753f, 15.853f, 9.211f, 16.009f, 9.68f, 16f)
                horizontalLineTo(19.4f)
                curveTo(19.869f, 16.009f, 20.327f, 15.853f, 20.692f, 15.558f)
                curveTo(21.058f, 15.264f, 21.309f, 14.85f, 21.4f, 14.39f)
                lineTo(23f, 6f)
                horizontalLineTo(6f)
            }
        }.build()

        return _ShoppingCart!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingCart: ImageVector? = null
