package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.StatDown: ImageVector
    get() {
        if (_StatDown != null) {
            return _StatDown!!
        }
        _StatDown = ImageVector.Builder(
            name = "StatDown",
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
                moveTo(4f, 10f)
                lineTo(12f, 18f)
                lineTo(15f, 15f)
                lineTo(20f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 12f)
                lineTo(13f, 9f)
                moveTo(16f, 4f)
                verticalLineTo(12f)
                verticalLineTo(4f)
                close()
                moveTo(16f, 12f)
                lineTo(19f, 9f)
                lineTo(16f, 12f)
                close()
            }
        }.build()

        return _StatDown!!
    }

@Suppress("ObjectPropertyName")
private var _StatDown: ImageVector? = null
