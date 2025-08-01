package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Package: ImageVector
    get() {
        if (_Package != null) {
            return _Package!!
        }
        _Package = ImageVector.Builder(
            name = "Package",
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
                moveTo(20f, 6f)
                verticalLineTo(18f)
                curveTo(20f, 19.105f, 19.105f, 20f, 18f, 20f)
                horizontalLineTo(6f)
                curveTo(4.895f, 20f, 4f, 19.105f, 4f, 18f)
                verticalLineTo(6f)
                curveTo(4f, 4.895f, 4.895f, 4f, 6f, 4f)
                horizontalLineTo(18f)
                curveTo(19.104f, 4f, 20f, 4.895f, 20f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(4f)
            }
        }.build()

        return _Package!!
    }

@Suppress("ObjectPropertyName")
private var _Package: ImageVector? = null
