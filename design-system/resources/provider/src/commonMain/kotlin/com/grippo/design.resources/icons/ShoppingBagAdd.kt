package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShoppingBagAdd: ImageVector
    get() {
        if (_ShoppingBagAdd != null) {
            return _ShoppingBagAdd!!
        }
        _ShoppingBagAdd = ImageVector.Builder(
            name = "ShoppingBagAdd",
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
                moveTo(14f, 5f)
                curveTo(14f, 3.895f, 13.105f, 3f, 12f, 3f)
                curveTo(10.895f, 3f, 10f, 3.895f, 10f, 5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.992f, 15f)
                verticalLineTo(18f)
                moveTo(8.992f, 15f)
                horizontalLineTo(11.992f)
                horizontalLineTo(8.992f)
                close()
                moveTo(14.992f, 15f)
                horizontalLineTo(11.992f)
                horizontalLineTo(14.992f)
                close()
                moveTo(11.992f, 15f)
                verticalLineTo(12f)
                verticalLineTo(15f)
                close()
            }
        }.build()

        return _ShoppingBagAdd!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingBagAdd: ImageVector? = null
