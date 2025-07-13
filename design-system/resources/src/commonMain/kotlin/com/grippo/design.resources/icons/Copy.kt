package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Copy: ImageVector
    get() {
        if (_Copy != null) {
            return _Copy!!
        }
        _Copy = ImageVector.Builder(
            name = "Copy",
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
                moveTo(19.4f, 20f)
                horizontalLineTo(9.6f)
                curveTo(9.269f, 20f, 9f, 19.731f, 9f, 19.4f)
                verticalLineTo(9.6f)
                curveTo(9f, 9.269f, 9.269f, 9f, 9.6f, 9f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 9f, 20f, 9.269f, 20f, 9.6f)
                verticalLineTo(19.4f)
                curveTo(20f, 19.731f, 19.731f, 20f, 19.4f, 20f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 9f)
                verticalLineTo(4.6f)
                curveTo(15f, 4.269f, 14.731f, 4f, 14.4f, 4f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 4f, 4f, 4.269f, 4f, 4.6f)
                verticalLineTo(14.4f)
                curveTo(4f, 14.731f, 4.269f, 15f, 4.6f, 15f)
                horizontalLineTo(9f)
            }
        }.build()

        return _Copy!!
    }

@Suppress("ObjectPropertyName")
private var _Copy: ImageVector? = null
