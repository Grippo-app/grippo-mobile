package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Framer: ImageVector
    get() {
        if (_Framer != null) {
            return _Framer!!
        }
        _Framer = ImageVector.Builder(
            name = "Framer",
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
                moveTo(5f, 16f)
                horizontalLineTo(12f)
                moveTo(5f, 16f)
                verticalLineTo(9f)
                horizontalLineTo(19f)
                verticalLineTo(2f)
                horizontalLineTo(5f)
                lineTo(19f, 16f)
                horizontalLineTo(12f)
                horizontalLineTo(5f)
                close()
                moveTo(5f, 16f)
                lineTo(12f, 23f)
                verticalLineTo(16f)
                horizontalLineTo(5f)
                close()
            }
        }.build()

        return _Framer!!
    }

@Suppress("ObjectPropertyName")
private var _Framer: ImageVector? = null
