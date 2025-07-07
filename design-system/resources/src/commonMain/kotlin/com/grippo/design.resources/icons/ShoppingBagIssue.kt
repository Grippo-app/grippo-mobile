package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShoppingBagIssue: ImageVector
    get() {
        if (_ShoppingBagIssue != null) {
            return _ShoppingBagIssue!!
        }
        _ShoppingBagIssue = ImageVector.Builder(
            name = "ShoppingBagIssue",
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
                moveTo(20f, 14.5f)
                lineTo(19.261f, 9.696f)
                curveTo(19.111f, 8.72f, 18.271f, 8f, 17.284f, 8f)
                horizontalLineTo(6.716f)
                curveTo(5.729f, 8f, 4.889f, 8.72f, 4.739f, 9.696f)
                lineTo(3.355f, 18.696f)
                curveTo(3.168f, 19.907f, 4.105f, 21f, 5.331f, 21f)
                horizontalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 17f)
                verticalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 22.01f)
                lineTo(17.51f, 21.999f)
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

        return _ShoppingBagIssue!!
    }

@Suppress("ObjectPropertyName")
private var _ShoppingBagIssue: ImageVector? = null
