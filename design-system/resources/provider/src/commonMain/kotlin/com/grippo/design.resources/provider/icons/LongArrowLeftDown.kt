package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LongArrowLeftDown: ImageVector
    get() {
        if (_LongArrowLeftDown != null) {
            return _LongArrowLeftDown!!
        }
        _LongArrowLeftDown = ImageVector.Builder(
            name = "LongArrowLeftDown",
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
                moveTo(4.5f, 13.5f)
                lineTo(8f, 17f)
                lineTo(11.5f, 13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 17f)
                verticalLineTo(11f)
                curveTo(8f, 8.791f, 9.791f, 7f, 12f, 7f)
                horizontalLineTo(19f)
            }
        }.build()

        return _LongArrowLeftDown!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowLeftDown: ImageVector? = null
