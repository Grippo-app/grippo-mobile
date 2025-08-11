package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EvStation: ImageVector
    get() {
        if (_EvStation != null) {
            return _EvStation!!
        }
        _EvStation = ImageVector.Builder(
            name = "EvStation",
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
                moveTo(21f, 5f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(5f, 19f)
                verticalLineTo(9f)
                curveTo(5f, 7.895f, 5.895f, 7f, 7f, 7f)
                horizontalLineTo(16f)
                curveTo(17.105f, 7f, 18f, 7.895f, 18f, 9f)
                verticalLineTo(19f)
                curveTo(18f, 20.105f, 17.105f, 21f, 16f, 21f)
                horizontalLineTo(7f)
                curveTo(5.895f, 21f, 5f, 20.105f, 5f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(5f, 10f)
                verticalLineTo(5f)
                curveTo(5f, 3.895f, 5.895f, 3f, 7f, 3f)
                horizontalLineTo(16f)
                curveTo(17.105f, 3f, 18f, 3.895f, 18f, 5f)
                verticalLineTo(10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.167f, 11f)
                lineTo(9.5f, 14f)
                horizontalLineTo(13.5f)
                lineTo(11.833f, 17f)
            }
        }.build()

        return _EvStation!!
    }

@Suppress("ObjectPropertyName")
private var _EvStation: ImageVector? = null
