package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Enlarge: ImageVector
    get() {
        if (_Enlarge != null) {
            return _Enlarge!!
        }
        _Enlarge = ImageVector.Builder(
            name = "Enlarge",
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
                moveTo(20f, 4f)
                horizontalLineTo(16f)
                moveTo(15f, 9f)
                lineTo(20f, 4f)
                lineTo(15f, 9f)
                close()
                moveTo(20f, 4f)
                verticalLineTo(8f)
                verticalLineTo(4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                horizontalLineTo(8f)
                moveTo(9f, 15f)
                lineTo(4f, 20f)
                lineTo(9f, 15f)
                close()
                moveTo(4f, 20f)
                verticalLineTo(16f)
                verticalLineTo(20f)
                close()
            }
        }.build()

        return _Enlarge!!
    }

@Suppress("ObjectPropertyName")
private var _Enlarge: ImageVector? = null
