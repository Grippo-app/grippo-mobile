package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Tiktok: ImageVector
    get() {
        if (_Tiktok != null) {
            return _Tiktok!!
        }
        _Tiktok = ImageVector.Builder(
            name = "Tiktok",
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
                moveTo(21f, 8f)
                verticalLineTo(16f)
                curveTo(21f, 18.761f, 18.761f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(5.239f, 21f, 3f, 18.761f, 3f, 16f)
                verticalLineTo(8f)
                curveTo(3f, 5.239f, 5.239f, 3f, 8f, 3f)
                horizontalLineTo(16f)
                curveTo(18.761f, 3f, 21f, 5.239f, 21f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 12f)
                curveTo(8.343f, 12f, 7f, 13.343f, 7f, 15f)
                curveTo(7f, 16.657f, 8.343f, 18f, 10f, 18f)
                curveTo(11.657f, 18f, 13f, 16.657f, 13f, 15f)
                verticalLineTo(6f)
                curveTo(13.333f, 7f, 14.6f, 9f, 17f, 9f)
            }
        }.build()

        return _Tiktok!!
    }

@Suppress("ObjectPropertyName")
private var _Tiktok: ImageVector? = null
