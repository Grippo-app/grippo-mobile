package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FontSize: ImageVector
    get() {
        if (_FontSize != null) {
            return _FontSize!!
        }
        _FontSize = ImageVector.Builder(
            name = "FontSize",
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
                moveTo(18f, 11f)
                lineTo(20f, 13f)
                moveTo(18f, 21f)
                verticalLineTo(11f)
                verticalLineTo(21f)
                close()
                moveTo(18f, 21f)
                lineTo(16f, 18.5f)
                lineTo(18f, 21f)
                close()
                moveTo(18f, 21f)
                lineTo(20f, 18.5f)
                lineTo(18f, 21f)
                close()
                moveTo(18f, 11f)
                lineTo(16f, 13f)
                lineTo(18f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 17f)
                horizontalLineTo(11f)
                moveTo(9f, 5f)
                verticalLineTo(17f)
                verticalLineTo(5f)
                close()
                moveTo(9f, 17f)
                horizontalLineTo(7f)
                horizontalLineTo(9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                verticalLineTo(7f)
            }
        }.build()

        return _FontSize!!
    }

@Suppress("ObjectPropertyName")
private var _FontSize: ImageVector? = null
