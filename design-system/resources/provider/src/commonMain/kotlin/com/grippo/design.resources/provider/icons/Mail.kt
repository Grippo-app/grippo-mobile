package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Mail: ImageVector
    get() {
        if (_Mail != null) {
            return _Mail!!
        }
        _Mail = ImageVector.Builder(
            name = "Mail",
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
                moveTo(5f, 6f)
                horizontalLineTo(19f)
                curveTo(20.105f, 6f, 21f, 6.895f, 21f, 8f)
                verticalLineTo(16f)
                curveTo(21f, 17.105f, 20.105f, 18f, 19f, 18f)
                horizontalLineTo(5f)
                curveTo(3.895f, 18f, 3f, 17.105f, 3f, 16f)
                verticalLineTo(8f)
                curveTo(3f, 6.895f, 3.895f, 6f, 5f, 6f)
                close()
                moveTo(3.5f, 7.5f)
                lineTo(10.8f, 12.975f)
                curveTo(11.511f, 13.508f, 12.489f, 13.508f, 13.2f, 12.975f)
                lineTo(20.5f, 7.5f)
            }
        }.build()

        return _Mail!!
    }

@Suppress("ObjectPropertyName")
private var _Mail: ImageVector? = null
