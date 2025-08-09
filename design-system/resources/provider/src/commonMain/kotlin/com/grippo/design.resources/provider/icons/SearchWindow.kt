package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SearchWindow: ImageVector
    get() {
        if (_SearchWindow != null) {
            return _SearchWindow!!
        }
        _SearchWindow = ImageVector.Builder(
            name = "SearchWindow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(13f, 21f)
                horizontalLineTo(4f)
                curveTo(2.895f, 21f, 2f, 20.105f, 2f, 19f)
                verticalLineTo(5f)
                curveTo(2f, 3.895f, 2.895f, 3f, 4f, 3f)
                horizontalLineTo(20f)
                curveTo(21.105f, 3f, 22f, 3.895f, 22f, 5f)
                verticalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 7f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 5.01f)
                lineTo(5.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 5.01f)
                lineTo(8.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 5.01f)
                lineTo(11.01f, 4.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.124f, 20.118f)
                lineTo(22f, 22f)
                moveTo(20.124f, 20.118f)
                curveTo(20.665f, 19.576f, 21f, 18.827f, 21f, 18f)
                curveTo(21f, 16.343f, 19.657f, 15f, 18f, 15f)
                curveTo(16.343f, 15f, 15f, 16.343f, 15f, 18f)
                curveTo(15f, 19.657f, 16.343f, 21f, 18f, 21f)
                curveTo(18.83f, 21f, 19.581f, 20.663f, 20.124f, 20.118f)
                close()
            }
        }.build()

        return _SearchWindow!!
    }

@Suppress("ObjectPropertyName")
private var _SearchWindow: ImageVector? = null
