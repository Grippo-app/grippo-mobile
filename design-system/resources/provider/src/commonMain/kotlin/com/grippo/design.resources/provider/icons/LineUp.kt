package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LineUp: ImageVector
    get() {
        if (_LineUp != null) {
            return _LineUp!!
        }
        _LineUp = ImageVector.Builder(
            name = "LineUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 4f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                moveTo(8f, 15.5f)
                lineTo(11.5f, 12f)
                lineTo(14.5f, 14f)
                lineTo(20f, 6f)
                moveTo(15.5f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(10.5f)
            }
        }.build()

        return _LineUp!!
    }

@Suppress("ObjectPropertyName")
private var _LineUp: ImageVector? = null
