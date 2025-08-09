package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Import: ImageVector
    get() {
        if (_Import != null) {
            return _Import!!
        }
        _Import = ImageVector.Builder(
            name = "Import",
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
                moveTo(4f, 13f)
                verticalLineTo(19f)
                curveTo(4f, 20.105f, 4.895f, 21f, 6f, 21f)
                horizontalLineTo(18f)
                curveTo(19.105f, 21f, 20f, 20.105f, 20f, 19f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                lineTo(15.5f, 11.5f)
                moveTo(12f, 3f)
                verticalLineTo(15f)
                verticalLineTo(3f)
                close()
                moveTo(12f, 15f)
                lineTo(8.5f, 11.5f)
                lineTo(12f, 15f)
                close()
            }
        }.build()

        return _Import!!
    }

@Suppress("ObjectPropertyName")
private var _Import: ImageVector? = null
