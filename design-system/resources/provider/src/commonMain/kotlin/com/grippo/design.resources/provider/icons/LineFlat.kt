package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LineFlat: ImageVector
    get() {
        if (_LineFlat != null) {
            return _LineFlat!!
        }
        _LineFlat = ImageVector.Builder(
            name = "LineFlat",
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
                moveTo(4f, 4f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                moveTo(8f, 12f)
                horizontalLineTo(20f)
                moveTo(16f, 8f)
                lineTo(20f, 12f)
                lineTo(16f, 16f)
            }
        }.build()

        return _LineFlat!!
    }

@Suppress("ObjectPropertyName")
private var _LineFlat: ImageVector? = null
