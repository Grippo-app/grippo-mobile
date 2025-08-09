package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShoppingBagArrowUp: ImageVector
    get() {
        if (_ShoppingBagArrowUp != null) {
            return _ShoppingBagArrowUp!!
        }
        _ShoppingBagArrowUp = ImageVector.Builder(
            name = "ShoppingBagArrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 21f)
                horizontalLineTo(18.669f)
                curveTo(19.895f, 21f, 20.832f, 19.907f, 20.646f, 18.696f)
                lineTo(19.261f, 9.696f)
                curveTo(19.111f, 8.72f, 18.271f, 8f, 17.284f, 8f)
                horizontalLineTo(6.716f)
                curveTo(5.729f, 8f, 4.889f, 8.72f, 4.739f, 9.696f)
                lineTo(3.355f, 18.696f)
                curveTo(3.168f, 19.907f, 4.105f, 21f, 5.331f, 21f)
                horizontalLineTo(7.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12f)
                lineTo(9f, 15f)
                moveTo(12f, 19f)
                verticalLineTo(12f)
                verticalLineTo(19f)
                close()
                moveTo(12f, 12f)
                lineTo(15f, 15f)
                lineTo(12f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 5f)
                curveTo(14f, 3.895f, 13.105f, 3f, 12f, 3f)
                curveTo(10.895f, 3f, 10f, 3.895f, 10f, 5f)
            }
        }.build()

        return _ShoppingBagArrowUp!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingBagArrowUp: ImageVector? = null
