package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.DollarSign: ImageVector
    get() {
        if (_DollarSign != null) {
            return _DollarSign!!
        }
        _DollarSign = ImageVector.Builder(
            name = "DollarSign",
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
                moveTo(12f, 1f)
                verticalLineTo(23f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 5f)
                horizontalLineTo(9.5f)
                curveTo(8.572f, 5f, 7.681f, 5.369f, 7.025f, 6.025f)
                curveTo(6.369f, 6.681f, 6f, 7.572f, 6f, 8.5f)
                curveTo(6f, 9.428f, 6.369f, 10.318f, 7.025f, 10.975f)
                curveTo(7.681f, 11.631f, 8.572f, 12f, 9.5f, 12f)
                horizontalLineTo(14.5f)
                curveTo(15.428f, 12f, 16.319f, 12.369f, 16.975f, 13.025f)
                curveTo(17.631f, 13.682f, 18f, 14.572f, 18f, 15.5f)
                curveTo(18f, 16.428f, 17.631f, 17.319f, 16.975f, 17.975f)
                curveTo(16.319f, 18.631f, 15.428f, 19f, 14.5f, 19f)
                horizontalLineTo(6f)
            }
        }.build()

        return _DollarSign!!
    }

@Suppress("ObjectPropertyName")
private var _DollarSign: ImageVector? = null
