package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.CableRounded: ImageVector
    get() {
        if (_CableRounded != null) {
            return _CableRounded!!
        }
        _CableRounded = ImageVector.Builder(
            name = "CableRounded",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 5.686f, 4.686f, 3f, 8f, 3f)
                horizontalLineTo(16f)
                curveTo(19.314f, 3f, 22f, 5.686f, 22f, 9f)
                verticalLineTo(15f)
                curveTo(22f, 18.314f, 19.314f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(4.686f, 21f, 2f, 18.314f, 2f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.667f, 8f)
                lineTo(10f, 12f)
                horizontalLineTo(14f)
                lineTo(12.333f, 16f)
            }
        }.build()

        return _CableRounded!!
    }

@Suppress("ObjectPropertyName")
private var _CableRounded: ImageVector? = null
