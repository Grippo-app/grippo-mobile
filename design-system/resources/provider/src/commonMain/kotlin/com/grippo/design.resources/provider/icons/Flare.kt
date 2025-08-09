package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Flare: ImageVector
    get() {
        if (_Flare != null) {
            return _Flare!!
        }
        _Flare = ImageVector.Builder(
            name = "Flare",
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
                moveTo(11.456f, 2.665f)
                curveTo(11.672f, 2.203f, 12.328f, 2.203f, 12.544f, 2.665f)
                lineTo(15.408f, 8.802f)
                curveTo(15.467f, 8.93f, 15.57f, 9.033f, 15.698f, 9.092f)
                lineTo(21.835f, 11.956f)
                curveTo(22.297f, 12.172f, 22.297f, 12.828f, 21.835f, 13.044f)
                lineTo(15.698f, 15.908f)
                curveTo(15.57f, 15.967f, 15.467f, 16.07f, 15.408f, 16.198f)
                lineTo(12.544f, 22.335f)
                curveTo(12.328f, 22.797f, 11.672f, 22.797f, 11.456f, 22.335f)
                lineTo(8.592f, 16.198f)
                curveTo(8.533f, 16.07f, 8.43f, 15.967f, 8.302f, 15.908f)
                lineTo(2.165f, 13.044f)
                curveTo(1.703f, 12.828f, 1.703f, 12.172f, 2.165f, 11.956f)
                lineTo(8.302f, 9.092f)
                curveTo(8.43f, 9.033f, 8.533f, 8.93f, 8.592f, 8.802f)
                lineTo(11.456f, 2.665f)
                close()
            }
        }.build()

        return _Flare!!
    }

@Suppress("ObjectPropertyName")
private var _Flare: ImageVector? = null
