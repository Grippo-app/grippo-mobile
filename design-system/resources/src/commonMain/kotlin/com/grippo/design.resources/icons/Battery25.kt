package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Battery25: ImageVector
    get() {
        if (_Battery25 != null) {
            return _Battery25!!
        }
        _Battery25 = ImageVector.Builder(
            name = "Battery25",
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
                moveTo(23f, 10f)
                verticalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(1f, 16f)
                verticalLineTo(8f)
                curveTo(1f, 6.895f, 1.895f, 6f, 3f, 6f)
                horizontalLineTo(18f)
                curveTo(19.105f, 6f, 20f, 6.895f, 20f, 8f)
                verticalLineTo(16f)
                curveTo(20f, 17.105f, 19.105f, 18f, 18f, 18f)
                horizontalLineTo(3f)
                curveTo(1.895f, 18f, 1f, 17.105f, 1f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(4f, 14.4f)
                verticalLineTo(9.6f)
                curveTo(4f, 9.269f, 4.269f, 9f, 4.6f, 9f)
                horizontalLineTo(6.4f)
                curveTo(6.731f, 9f, 7f, 9.269f, 7f, 9.6f)
                verticalLineTo(14.4f)
                curveTo(7f, 14.731f, 6.731f, 15f, 6.4f, 15f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 15f, 4f, 14.731f, 4f, 14.4f)
                close()
            }
        }.build()

        return _Battery25!!
    }

@Suppress("ObjectPropertyName")
private var _Battery25: ImageVector? = null
