package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Megaphone: ImageVector
    get() {
        if (_Megaphone != null) {
            return _Megaphone!!
        }
        _Megaphone = ImageVector.Builder(
            name = "Megaphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(14f, 14f)
                horizontalLineTo(7f)
                curveTo(4.791f, 14f, 3f, 12.209f, 3f, 10f)
                curveTo(3f, 7.791f, 4.791f, 6f, 7f, 6f)
                horizontalLineTo(14f)
                moveTo(14f, 14f)
                verticalLineTo(6f)
                verticalLineTo(14f)
                close()
                moveTo(14f, 14f)
                lineTo(20.102f, 17.487f)
                curveTo(20.502f, 17.716f, 21f, 17.427f, 21f, 16.966f)
                verticalLineTo(3.034f)
                curveTo(21f, 2.573f, 20.502f, 2.284f, 20.102f, 2.513f)
                lineTo(14f, 6f)
                verticalLineTo(14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7.757f, 19.3f)
                lineTo(7f, 14f)
                horizontalLineTo(11f)
                lineTo(11.677f, 18.74f)
                curveTo(11.848f, 19.933f, 10.922f, 21f, 9.717f, 21f)
                curveTo(8.732f, 21f, 7.897f, 20.275f, 7.757f, 19.3f)
                close()
            }
        }.build()

        return _Megaphone!!
    }

@Suppress("ObjectPropertyName")
private var _Megaphone: ImageVector? = null
