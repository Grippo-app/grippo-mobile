package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.RepeatOnce: ImageVector
    get() {
        if (_RepeatOnce != null) {
            return _RepeatOnce!!
        }
        _RepeatOnce = ImageVector.Builder(
            name = "RepeatOnce",
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
                moveTo(17f, 17f)
                horizontalLineTo(8f)
                curveTo(6.333f, 17f, 3f, 16f, 3f, 12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 7f)
                horizontalLineTo(16f)
                curveTo(17.667f, 7f, 21f, 8f, 21f, 12f)
                curveTo(21f, 13.494f, 20.535f, 14.57f, 19.865f, 15.331f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 14.5f)
                lineTo(17f, 17f)
                lineTo(14.5f, 19.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 8f)
                verticalLineTo(5f)
                verticalLineTo(3f)
                lineTo(2f, 4f)
            }
        }.build()

        return _RepeatOnce!!
    }

@Suppress("ObjectPropertyName")
private var _RepeatOnce: ImageVector? = null
