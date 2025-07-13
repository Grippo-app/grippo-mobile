package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AppNotification: ImageVector
    get() {
        if (_AppNotification != null) {
            return _AppNotification!!
        }
        _AppNotification = ImageVector.Builder(
            name = "AppNotification",
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
                moveTo(19f, 8f)
                curveTo(20.657f, 8f, 22f, 6.657f, 22f, 5f)
                curveTo(22f, 3.343f, 20.657f, 2f, 19f, 2f)
                curveTo(17.343f, 2f, 16f, 3.343f, 16f, 5f)
                curveTo(16f, 6.657f, 17.343f, 8f, 19f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                verticalLineTo(15f)
                curveTo(21f, 18.314f, 18.314f, 21f, 15f, 21f)
                horizontalLineTo(9f)
                curveTo(5.686f, 21f, 3f, 18.314f, 3f, 15f)
                verticalLineTo(9f)
                curveTo(3f, 5.686f, 5.686f, 3f, 9f, 3f)
                horizontalLineTo(12f)
            }
        }.build()

        return _AppNotification!!
    }

@Suppress("ObjectPropertyName")
private var _AppNotification: ImageVector? = null
