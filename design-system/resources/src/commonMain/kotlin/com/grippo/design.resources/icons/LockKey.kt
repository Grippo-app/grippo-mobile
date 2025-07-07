package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LockKey: ImageVector
    get() {
        if (_LockKey != null) {
            return _LockKey!!
        }
        _LockKey = ImageVector.Builder(
            name = "LockKey",
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
                moveTo(14.667f, 12f)
                horizontalLineTo(9.333f)
                moveTo(14.667f, 12f)
                horizontalLineTo(15.4f)
                curveTo(15.731f, 12f, 16f, 12.269f, 16f, 12.6f)
                verticalLineTo(16.4f)
                curveTo(16f, 16.731f, 15.731f, 17f, 15.4f, 17f)
                horizontalLineTo(8.6f)
                curveTo(8.269f, 17f, 8f, 16.731f, 8f, 16.4f)
                verticalLineTo(12.6f)
                curveTo(8f, 12.269f, 8.269f, 12f, 8.6f, 12f)
                horizontalLineTo(9.333f)
                horizontalLineTo(14.667f)
                close()
                moveTo(14.667f, 12f)
                verticalLineTo(9.5f)
                curveTo(14.667f, 8.667f, 14.133f, 7f, 12f, 7f)
                curveTo(9.867f, 7f, 9.333f, 8.667f, 9.333f, 9.5f)
                verticalLineTo(12f)
                horizontalLineTo(14.667f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(19f)
                curveTo(21f, 20.105f, 20.105f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                close()
            }
        }.build()

        return _LockKey!!
    }

@Suppress("ObjectPropertyName")
private var _LockKey: ImageVector? = null
