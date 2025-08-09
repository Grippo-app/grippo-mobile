package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.OnRounded: ImageVector
    get() {
        if (_OnRounded != null) {
            return _OnRounded!!
        }
        _OnRounded = ImageVector.Builder(
            name = "OnRounded",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(1f, 15f)
                verticalLineTo(9f)
                curveTo(1f, 5.686f, 3.686f, 3f, 7f, 3f)
                horizontalLineTo(17f)
                curveTo(20.314f, 3f, 23f, 5.686f, 23f, 9f)
                verticalLineTo(15f)
                curveTo(23f, 18.314f, 20.314f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(3.686f, 21f, 1f, 18.314f, 1f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(9f, 9f)
                curveTo(10.657f, 9f, 12f, 10.343f, 12f, 12f)
                curveTo(12f, 13.657f, 10.657f, 15f, 9f, 15f)
                curveTo(7.343f, 15f, 6f, 13.657f, 6f, 12f)
                curveTo(6f, 10.343f, 7.343f, 9f, 9f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 15f)
                verticalLineTo(9f)
                lineTo(18f, 15f)
                verticalLineTo(9f)
            }
        }.build()

        return _OnRounded!!
    }

@Suppress("ObjectPropertyName")
private var _OnRounded: ImageVector? = null
