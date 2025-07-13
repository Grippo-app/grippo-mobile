package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LongArrowRightDown: ImageVector
    get() {
        if (_LongArrowRightDown != null) {
            return _LongArrowRightDown!!
        }
        _LongArrowRightDown = ImageVector.Builder(
            name = "LongArrowRightDown",
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
                moveTo(19f, 13.5f)
                lineTo(15.5f, 17f)
                lineTo(12f, 13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 17f)
                verticalLineTo(11f)
                curveTo(15.5f, 8.791f, 13.709f, 7f, 11.5f, 7f)
                horizontalLineTo(4.5f)
            }
        }.build()

        return _LongArrowRightDown!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowRightDown: ImageVector? = null
