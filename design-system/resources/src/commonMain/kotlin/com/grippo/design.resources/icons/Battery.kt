package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Battery: ImageVector
    get() {
        if (_Battery != null) {
            return _Battery!!
        }
        _Battery = ImageVector.Builder(
            name = "Battery",
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
                moveTo(17f, 6f)
                horizontalLineTo(3f)
                curveTo(1.895f, 6f, 1f, 6.895f, 1f, 8f)
                verticalLineTo(16f)
                curveTo(1f, 17.105f, 1.895f, 18f, 3f, 18f)
                horizontalLineTo(17f)
                curveTo(18.105f, 18f, 19f, 17.105f, 19f, 16f)
                verticalLineTo(8f)
                curveTo(19f, 6.895f, 18.105f, 6f, 17f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 13f)
                verticalLineTo(11f)
            }
        }.build()

        return _Battery!!
    }

@Suppress("ObjectPropertyName")
private var _Battery: ImageVector? = null
