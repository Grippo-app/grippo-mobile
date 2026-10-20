package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Stack: ImageVector
    get() {
        if (_Stack != null) {
            return _Stack!!
        }
        _Stack = ImageVector.Builder(
            name = "Stack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                lineTo(21f, 8f)
                lineTo(12f, 13f)
                lineTo(3f, 8f)
                close()
                moveTo(3f, 12f)
                lineTo(12f, 17f)
                lineTo(21f, 12f)
                moveTo(3f, 16f)
                lineTo(12f, 21f)
                lineTo(21f, 16f)
            }
        }.build()

        return _Stack!!
    }

@Suppress("ObjectPropertyName")
private var _Stack: ImageVector? = null
