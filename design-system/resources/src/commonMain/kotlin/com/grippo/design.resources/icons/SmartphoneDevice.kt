package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.SmartphoneDevice: ImageVector
    get() {
        if (_SmartphoneDevice != null) {
            return _SmartphoneDevice!!
        }
        _SmartphoneDevice = ImageVector.Builder(
            name = "SmartphoneDevice",
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
                moveTo(12f, 16.01f)
                lineTo(12.01f, 15.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7f, 19.4f)
                verticalLineTo(4.6f)
                curveTo(7f, 4.269f, 7.269f, 4f, 7.6f, 4f)
                horizontalLineTo(16.4f)
                curveTo(16.731f, 4f, 17f, 4.269f, 17f, 4.6f)
                verticalLineTo(19.4f)
                curveTo(17f, 19.731f, 16.731f, 20f, 16.4f, 20f)
                horizontalLineTo(7.6f)
                curveTo(7.269f, 20f, 7f, 19.731f, 7f, 19.4f)
                close()
            }
        }.build()

        return _SmartphoneDevice!!
    }

@Suppress("ObjectPropertyName")
private var _SmartphoneDevice: ImageVector? = null
