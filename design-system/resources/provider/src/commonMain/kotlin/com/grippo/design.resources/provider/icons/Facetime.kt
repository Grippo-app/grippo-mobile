package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Facetime: ImageVector
    get() {
        if (_Facetime != null) {
            return _Facetime!!
        }
        _Facetime = ImageVector.Builder(
            name = "Facetime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 16f)
                verticalLineTo(8f)
                curveTo(2f, 4.686f, 4.686f, 2f, 8f, 2f)
                horizontalLineTo(16f)
                curveTo(19.314f, 2f, 22f, 4.686f, 22f, 8f)
                verticalLineTo(16f)
                curveTo(22f, 19.314f, 19.314f, 22f, 16f, 22f)
                horizontalLineTo(8f)
                curveTo(4.686f, 22f, 2f, 19.314f, 2f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6f, 13f)
                verticalLineTo(11f)
                curveTo(6f, 9.895f, 6.895f, 9f, 8f, 9f)
                horizontalLineTo(11f)
                curveTo(12.105f, 9f, 13f, 9.895f, 13f, 11f)
                verticalLineTo(13f)
                curveTo(13f, 14.105f, 12.105f, 15f, 11f, 15f)
                horizontalLineTo(8f)
                curveTo(6.895f, 15f, 6f, 14.105f, 6f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.04f, 9.22f)
                lineTo(13.973f, 11.52f)
                curveTo(13.653f, 11.76f, 13.653f, 12.24f, 13.973f, 12.48f)
                lineTo(17.04f, 14.78f)
                curveTo(17.436f, 15.077f, 18f, 14.794f, 18f, 14.3f)
                verticalLineTo(9.7f)
                curveTo(18f, 9.206f, 17.436f, 8.923f, 17.04f, 9.22f)
                close()
            }
        }.build()

        return _Facetime!!
    }

@Suppress("ObjectPropertyName")
private var _Facetime: ImageVector? = null
