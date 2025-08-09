package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.StatUp: ImageVector
    get() {
        if (_StatUp != null) {
            return _StatUp!!
        }
        _StatUp = ImageVector.Builder(
            name = "StatUp",
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
                lineTo(13f, 15f)
                moveTo(16f, 20f)
                verticalLineTo(12f)
                verticalLineTo(20f)
                close()
                moveTo(16f, 12f)
                lineTo(19f, 15f)
                lineTo(16f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 14f)
                lineTo(12f, 6f)
                lineTo(15f, 9f)
                lineTo(20f, 4f)
            }
        }.build()

        return _StatUp!!
    }

@Suppress("ObjectPropertyName")
private var _StatUp: ImageVector? = null
