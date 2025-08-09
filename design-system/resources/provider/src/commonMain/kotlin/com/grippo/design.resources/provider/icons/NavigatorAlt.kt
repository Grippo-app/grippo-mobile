package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.NavigatorAlt: ImageVector
    get() {
        if (_NavigatorAlt != null) {
            return _NavigatorAlt!!
        }
        _NavigatorAlt = ImageVector.Builder(
            name = "NavigatorAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(13.93f, 17.869f)
                curveTo(13.608f, 18.799f, 12.293f, 18.798f, 11.972f, 17.868f)
                lineTo(10.352f, 13.174f)
                lineTo(5.783f, 11.231f)
                curveTo(4.877f, 10.845f, 4.968f, 9.533f, 5.918f, 9.277f)
                lineTo(16.15f, 6.516f)
                curveTo(16.952f, 6.299f, 17.671f, 7.069f, 17.399f, 7.855f)
                lineTo(13.93f, 17.869f)
                close()
            }
        }.build()

        return _NavigatorAlt!!
    }

@Suppress("ObjectPropertyName")
private var _NavigatorAlt: ImageVector? = null
