package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ShortPantsAlt: ImageVector
    get() {
        if (_ShortPantsAlt != null) {
            return _ShortPantsAlt!!
        }
        _ShortPantsAlt = ImageVector.Builder(
            name = "ShortPantsAlt",
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
                moveTo(12f, 16.8f)
                horizontalLineTo(18.966f)
                curveTo(19.271f, 16.8f, 19.527f, 16.572f, 19.562f, 16.27f)
                lineTo(20.921f, 4.67f)
                curveTo(20.963f, 4.313f, 20.685f, 4f, 20.326f, 4f)
                horizontalLineTo(3.659f)
                curveTo(3.305f, 4f, 3.029f, 4.304f, 3.062f, 4.656f)
                lineTo(4.449f, 19.456f)
                curveTo(4.478f, 19.764f, 4.737f, 20f, 5.046f, 20f)
                horizontalLineTo(11.4f)
                curveTo(11.731f, 20f, 12f, 19.731f, 12f, 19.4f)
                verticalLineTo(12f)
            }
        }.build()

        return _ShortPantsAlt!!
    }

@Suppress("ObjectPropertyName")
private var _ShortPantsAlt: ImageVector? = null
