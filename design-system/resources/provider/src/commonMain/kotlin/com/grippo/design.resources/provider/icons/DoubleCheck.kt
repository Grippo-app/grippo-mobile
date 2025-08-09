package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DoubleCheck: ImageVector
    get() {
        if (_DoubleCheck != null) {
            return _DoubleCheck!!
        }
        _DoubleCheck = ImageVector.Builder(
            name = "DoubleCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(1.5f, 12.5f)
                lineTo(5.576f, 16.576f)
                curveTo(5.81f, 16.81f, 6.19f, 16.81f, 6.424f, 16.576f)
                lineTo(9f, 14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 7f)
                lineTo(12f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 12f)
                lineTo(11.576f, 16.576f)
                curveTo(11.81f, 16.81f, 12.19f, 16.81f, 12.424f, 16.576f)
                lineTo(22f, 7f)
            }
        }.build()

        return _DoubleCheck!!
    }

@Suppress("ObjectPropertyName")
private var _DoubleCheck: ImageVector? = null
