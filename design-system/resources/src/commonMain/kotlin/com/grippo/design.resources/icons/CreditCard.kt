package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CreditCard: ImageVector
    get() {
        if (_CreditCard != null) {
            return _CreditCard!!
        }
        _CreditCard = ImageVector.Builder(
            name = "CreditCard",
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
                moveTo(21f, 4f)
                horizontalLineTo(3f)
                curveTo(1.895f, 4f, 1f, 4.895f, 1f, 6f)
                verticalLineTo(18f)
                curveTo(1f, 19.105f, 1.895f, 20f, 3f, 20f)
                horizontalLineTo(21f)
                curveTo(22.105f, 20f, 23f, 19.105f, 23f, 18f)
                verticalLineTo(6f)
                curveTo(23f, 4.895f, 22.105f, 4f, 21f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 10f)
                horizontalLineTo(23f)
            }
        }.build()

        return _CreditCard!!
    }

@Suppress("ObjectPropertyName")
private var _CreditCard: ImageVector? = null
