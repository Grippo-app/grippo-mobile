package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Lock: ImageVector
    get() {
        if (_Lock != null) {
            return _Lock!!
        }
        _Lock = ImageVector.Builder(
            name = "Lock",
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
                moveTo(8f, 10f)
                verticalLineTo(7f)
                curveTo(8f, 4.791f, 9.791f, 3f, 12f, 3f)
                curveTo(14.209f, 3f, 16f, 4.791f, 16f, 7f)
                verticalLineTo(10f)
                moveTo(7f, 10f)
                horizontalLineTo(17f)
                curveTo(18.105f, 10f, 19f, 10.895f, 19f, 12f)
                verticalLineTo(18f)
                curveTo(19f, 19.105f, 18.105f, 20f, 17f, 20f)
                horizontalLineTo(7f)
                curveTo(5.895f, 20f, 5f, 19.105f, 5f, 18f)
                verticalLineTo(12f)
                curveTo(5f, 10.895f, 5.895f, 10f, 7f, 10f)
                close()
                moveTo(12f, 14f)
                verticalLineTo(16f)
            }
        }.build()

        return _Lock!!
    }

@Suppress("ObjectPropertyName")
private var _Lock: ImageVector? = null
