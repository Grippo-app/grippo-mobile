package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Archery: ImageVector
    get() {
        if (_Archery != null) {
            return _Archery!!
        }
        _Archery = ImageVector.Builder(
            name = "Archery",
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
                moveTo(18f, 12f)
                lineTo(16f, 14f)
                moveTo(7f, 12f)
                horizontalLineTo(18f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 12f)
                lineTo(5f, 10f)
                horizontalLineTo(1f)
                lineTo(3f, 12f)
                lineTo(1f, 14f)
                horizontalLineTo(5f)
                lineTo(7f, 12f)
                close()
                moveTo(18f, 12f)
                lineTo(16f, 10f)
                lineTo(18f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 22f)
                curveTo(20.538f, 22f, 23f, 17.523f, 23f, 12f)
                curveTo(23f, 6.477f, 20.538f, 2f, 17.5f, 2f)
                curveTo(14.462f, 2f, 12f, 6.477f, 12f, 12f)
                curveTo(12f, 17.523f, 14.462f, 22f, 17.5f, 22f)
                close()
            }
        }.build()

        return _Archery!!
    }

@Suppress("ObjectPropertyName")
private var _Archery: ImageVector? = null
