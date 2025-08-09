package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Plus: ImageVector
    get() {
        if (_Plus != null) {
            return _Plus!!
        }
        _Plus = ImageVector.Builder(
            name = "Plus",
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
                moveTo(12f, 12f)
                verticalLineTo(18f)
                moveTo(6f, 12f)
                horizontalLineTo(12f)
                horizontalLineTo(6f)
                close()
                moveTo(18f, 12f)
                horizontalLineTo(12f)
                horizontalLineTo(18f)
                close()
                moveTo(12f, 12f)
                verticalLineTo(6f)
                verticalLineTo(12f)
                close()
            }
        }.build()

        return _Plus!!
    }

@Suppress("ObjectPropertyName")
private var _Plus: ImageVector? = null
