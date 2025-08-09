package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LongArrowRightUp: ImageVector
    get() {
        if (_LongArrowRightUp != null) {
            return _LongArrowRightUp!!
        }
        _LongArrowRightUp = ImageVector.Builder(
            name = "LongArrowRightUp",
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
                moveTo(19f, 10.5f)
                lineTo(15.5f, 7f)
                lineTo(12f, 10.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 7f)
                verticalLineTo(13f)
                curveTo(15.5f, 15.209f, 13.709f, 17f, 11.5f, 17f)
                horizontalLineTo(4.5f)
            }
        }.build()

        return _LongArrowRightUp!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowRightUp: ImageVector? = null
