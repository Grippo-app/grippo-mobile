package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.TrendingUp: ImageVector
    get() {
        if (_TrendingUp != null) {
            return _TrendingUp!!
        }
        _TrendingUp = ImageVector.Builder(
            name = "TrendingUp",
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
                moveTo(23f, 6f)
                lineTo(13.5f, 15.5f)
                lineTo(8.5f, 10.5f)
                lineTo(1f, 18f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 6f)
                horizontalLineTo(23f)
                verticalLineTo(12f)
            }
        }.build()

        return _TrendingUp!!
    }

@Suppress("ObjectPropertyName")
private var _TrendingUp: ImageVector? = null
