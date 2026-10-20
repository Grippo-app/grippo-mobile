package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Logout: ImageVector
    get() {
        if (_Logout != null) {
            return _Logout!!
        }
        _Logout = ImageVector.Builder(
            name = "Logout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 7f)
                verticalLineTo(6f)
                curveTo(12f, 4.895f, 11.105f, 4f, 10f, 4f)
                horizontalLineTo(6f)
                curveTo(4.895f, 4f, 4f, 4.895f, 4f, 6f)
                verticalLineTo(18f)
                curveTo(4f, 19.105f, 4.895f, 20f, 6f, 20f)
                horizontalLineTo(10f)
                curveTo(11.105f, 20f, 12f, 19.105f, 12f, 18f)
                verticalLineTo(17f)
                moveTo(10f, 12f)
                horizontalLineTo(21f)
                moveTo(17f, 8f)
                lineTo(21f, 12f)
                lineTo(17f, 16f)
            }
        }.build()

        return _Logout!!
    }

@Suppress("ObjectPropertyName")
private var _Logout: ImageVector? = null
