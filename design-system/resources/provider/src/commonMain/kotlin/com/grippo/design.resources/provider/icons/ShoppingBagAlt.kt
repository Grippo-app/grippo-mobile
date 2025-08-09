package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ShoppingBagAlt: ImageVector
    get() {
        if (_ShoppingBagAlt != null) {
            return _ShoppingBagAlt!!
        }
        _ShoppingBagAlt = ImageVector.Builder(
            name = "ShoppingBagAlt",
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
                moveTo(19.261f, 9.696f)
                lineTo(20.646f, 18.696f)
                curveTo(20.832f, 19.907f, 19.895f, 21f, 18.669f, 21f)
                horizontalLineTo(5.331f)
                curveTo(4.105f, 21f, 3.168f, 19.907f, 3.355f, 18.696f)
                lineTo(4.739f, 9.696f)
                curveTo(4.889f, 8.72f, 5.729f, 8f, 6.716f, 8f)
                horizontalLineTo(17.284f)
                curveTo(18.271f, 8f, 19.111f, 8.72f, 19.261f, 9.696f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 11f)
                verticalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 11f)
                verticalLineTo(18f)
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

        return _ShoppingBagAlt!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingBagAlt: ImageVector? = null
