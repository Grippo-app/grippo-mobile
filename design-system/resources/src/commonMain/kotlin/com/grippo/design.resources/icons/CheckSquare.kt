package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CheckSquare: ImageVector
    get() {
        if (_CheckSquare != null) {
            return _CheckSquare!!
        }
        _CheckSquare = ImageVector.Builder(
            name = "CheckSquare",
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
                moveTo(9f, 11f)
                lineTo(12f, 14f)
                lineTo(22f, 4f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                verticalLineTo(19f)
                curveTo(21f, 19.53f, 20.789f, 20.039f, 20.414f, 20.414f)
                curveTo(20.039f, 20.789f, 19.53f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(4.47f, 21f, 3.961f, 20.789f, 3.586f, 20.414f)
                curveTo(3.211f, 20.039f, 3f, 19.53f, 3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 4.47f, 3.211f, 3.961f, 3.586f, 3.586f)
                curveTo(3.961f, 3.211f, 4.47f, 3f, 5f, 3f)
                horizontalLineTo(16f)
            }
        }.build()

        return _CheckSquare!!
    }

@Suppress("ObjectPropertyName")
private var _CheckSquare: ImageVector? = null
