package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RefreshDouble: ImageVector
    get() {
        if (_RefreshDouble != null) {
            return _RefreshDouble!!
        }
        _RefreshDouble = ImageVector.Builder(
            name = "RefreshDouble",
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
                moveTo(21.168f, 8f)
                curveTo(19.625f, 4.468f, 16.101f, 2f, 12f, 2f)
                curveTo(6.815f, 2f, 2.551f, 5.947f, 2.049f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 8f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 8f, 22f, 7.731f, 22f, 7.4f)
                verticalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.881f, 16f)
                curveTo(4.424f, 19.532f, 7.949f, 22f, 12.049f, 22f)
                curveTo(17.235f, 22f, 21.498f, 18.053f, 22f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.049f, 16f)
                horizontalLineTo(2.649f)
                curveTo(2.318f, 16f, 2.049f, 16.269f, 2.049f, 16.6f)
                verticalLineTo(21f)
            }
        }.build()

        return _RefreshDouble!!
    }

@Suppress("ObjectPropertyName")
private var _RefreshDouble: ImageVector? = null
