package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Bluetooth: ImageVector
    get() {
        if (_Bluetooth != null) {
            return _Bluetooth!!
        }
        _Bluetooth = ImageVector.Builder(
            name = "Bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                lineTo(12f, 23f)
                verticalLineTo(1f)
                lineTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }.build()

        return _Bluetooth!!
    }

@Suppress("ObjectPropertyName")
private var _Bluetooth: ImageVector? = null
