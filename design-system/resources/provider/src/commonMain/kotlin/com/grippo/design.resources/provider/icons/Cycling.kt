package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Cycling: ImageVector
    get() {
        if (_Cycling != null) {
            return _Cycling!!
        }
        _Cycling = ImageVector.Builder(
            name = "Cycling",
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
                moveTo(14f, 7f)
                curveTo(15.105f, 7f, 16f, 6.105f, 16f, 5f)
                curveTo(16f, 3.895f, 15.105f, 3f, 14f, 3f)
                curveTo(12.895f, 3f, 12f, 3.895f, 12f, 5f)
                curveTo(12f, 6.105f, 12.895f, 7f, 14f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 21f)
                curveTo(19.657f, 21f, 21f, 19.657f, 21f, 18f)
                curveTo(21f, 16.343f, 19.657f, 15f, 18f, 15f)
                curveTo(16.343f, 15f, 15f, 16.343f, 15f, 18f)
                curveTo(15f, 19.657f, 16.343f, 21f, 18f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                curveTo(7.657f, 21f, 9f, 19.657f, 9f, 18f)
                curveTo(9f, 16.343f, 7.657f, 15f, 6f, 15f)
                curveTo(4.343f, 15f, 3f, 16.343f, 3f, 18f)
                curveTo(3f, 19.657f, 4.343f, 21f, 6f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.5f, 18f)
                lineTo(13f, 14f)
                lineTo(8.118f, 12f)
                lineTo(11.118f, 8.5f)
                lineTo(14.118f, 11f)
                horizontalLineTo(17.618f)
            }
        }.build()

        return _Cycling!!
    }

@Suppress("ObjectPropertyName")
private var _Cycling: ImageVector? = null
