package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Timer: ImageVector
    get() {
        if (_Timer != null) {
            return _Timer!!
        }
        _Timer = ImageVector.Builder(
            name = "Timer",
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
                moveTo(9f, 3f)
                horizontalLineTo(15f)
                moveTo(12f, 3f)
                verticalLineTo(5.5f)
                moveTo(17.5f, 7.5f)
                lineTo(19f, 6f)
                moveTo(12f, 5.5f)
                curveTo(16.142f, 5.5f, 19.5f, 8.858f, 19.5f, 13f)
                curveTo(19.5f, 17.142f, 16.142f, 20.5f, 12f, 20.5f)
                curveTo(7.858f, 20.5f, 4.5f, 17.142f, 4.5f, 13f)
                curveTo(4.5f, 8.858f, 7.858f, 5.5f, 12f, 5.5f)
                close()
                moveTo(12f, 9f)
                verticalLineTo(13f)
                lineTo(15f, 15f)
            }
        }.build()

        return _Timer!!
    }

@Suppress("ObjectPropertyName")
private var _Timer: ImageVector? = null
