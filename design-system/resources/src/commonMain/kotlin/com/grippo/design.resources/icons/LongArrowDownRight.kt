package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LongArrowDownRight: ImageVector
    get() {
        if (_LongArrowDownRight != null) {
            return _LongArrowDownRight!!
        }
        _LongArrowDownRight = ImageVector.Builder(
            name = "LongArrowDownRight",
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
                moveTo(13.25f, 19.25f)
                lineTo(16.75f, 15.75f)
                lineTo(13.25f, 12.25f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.75f, 15.75f)
                horizontalLineTo(10.75f)
                curveTo(8.541f, 15.75f, 6.75f, 13.959f, 6.75f, 11.75f)
                verticalLineTo(4.75f)
            }
        }.build()

        return _LongArrowDownRight!!
    }

@Suppress("ObjectPropertyName")
private var _LongArrowDownRight: ImageVector? = null
