package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LongArrowLeftUp: ImageVector
    get() {
        if (_LongArrowLeftUp != null) {
            return _LongArrowLeftUp!!
        }
        _LongArrowLeftUp = ImageVector.Builder(
            name = "LongArrowLeftUp",
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
                moveTo(4.5f, 10.5f)
                lineTo(8f, 7f)
                lineTo(11.5f, 10.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 7f)
                verticalLineTo(13f)
                curveTo(8f, 15.209f, 9.791f, 17f, 12f, 17f)
                horizontalLineTo(19f)
            }
        }.build()

        return _LongArrowLeftUp!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowLeftUp: ImageVector? = null
