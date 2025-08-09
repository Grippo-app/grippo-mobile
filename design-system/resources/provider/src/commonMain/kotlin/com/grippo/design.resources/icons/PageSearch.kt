package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PageSearch: ImageVector
    get() {
        if (_PageSearch != null) {
            return _PageSearch!!
        }
        _PageSearch = ImageVector.Builder(
            name = "PageSearch",
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
                moveTo(20f, 12f)
                verticalLineTo(5.749f)
                curveTo(20f, 5.589f, 19.937f, 5.437f, 19.824f, 5.324f)
                lineTo(16.676f, 2.176f)
                curveTo(16.563f, 2.063f, 16.411f, 2f, 16.251f, 2f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 2f, 4f, 2.269f, 4f, 2.6f)
                verticalLineTo(21.4f)
                curveTo(4f, 21.731f, 4.269f, 22f, 4.6f, 22f)
                horizontalLineTo(11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 14f)
                horizontalLineTo(11f)
                moveTo(8f, 10f)
                horizontalLineTo(16f)
                horizontalLineTo(8f)
                close()
                moveTo(8f, 6f)
                horizontalLineTo(12f)
                horizontalLineTo(8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 20.5f)
                lineTo(22f, 22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 18f)
                curveTo(15f, 19.657f, 16.343f, 21f, 18f, 21f)
                curveTo(18.83f, 21f, 19.581f, 20.663f, 20.124f, 20.118f)
                curveTo(20.665f, 19.576f, 21f, 18.827f, 21f, 18f)
                curveTo(21f, 16.343f, 19.657f, 15f, 18f, 15f)
                curveTo(16.343f, 15f, 15f, 16.343f, 15f, 18f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5.4f)
                verticalLineTo(2.354f)
                curveTo(16f, 2.158f, 16.158f, 2f, 16.354f, 2f)
                curveTo(16.447f, 2f, 16.537f, 2.037f, 16.604f, 2.104f)
                lineTo(19.896f, 5.396f)
                curveTo(19.963f, 5.463f, 20f, 5.553f, 20f, 5.646f)
                curveTo(20f, 5.842f, 19.842f, 6f, 19.646f, 6f)
                horizontalLineTo(16.6f)
                curveTo(16.269f, 6f, 16f, 5.731f, 16f, 5.4f)
                close()
            }
        }.build()

        return _PageSearch!!
    }

@Suppress("ObjectPropertyName")
private var _PageSearch: ImageVector? = null
