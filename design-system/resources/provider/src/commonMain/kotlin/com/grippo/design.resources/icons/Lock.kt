package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Lock: ImageVector
    get() {
        if (_Lock != null) {
            return _Lock!!
        }
        _Lock = ImageVector.Builder(
            name = "Lock",
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
                moveTo(16f, 12f)
                horizontalLineTo(8f)
                moveTo(16f, 12f)
                horizontalLineTo(17.4f)
                curveTo(17.731f, 12f, 18f, 12.269f, 18f, 12.6f)
                verticalLineTo(19.4f)
                curveTo(18f, 19.731f, 17.731f, 20f, 17.4f, 20f)
                horizontalLineTo(6.6f)
                curveTo(6.269f, 20f, 6f, 19.731f, 6f, 19.4f)
                verticalLineTo(12.6f)
                curveTo(6f, 12.269f, 6.269f, 12f, 6.6f, 12f)
                horizontalLineTo(8f)
                horizontalLineTo(16f)
                close()
                moveTo(16f, 12f)
                verticalLineTo(8f)
                curveTo(16f, 6.667f, 15.2f, 4f, 12f, 4f)
                curveTo(8.8f, 4f, 8f, 6.667f, 8f, 8f)
                verticalLineTo(12f)
                horizontalLineTo(16f)
                close()
            }
        }.build()

        return _Lock!!
    }

@Suppress("ObjectPropertyName")
private var _Lock: ImageVector? = null
