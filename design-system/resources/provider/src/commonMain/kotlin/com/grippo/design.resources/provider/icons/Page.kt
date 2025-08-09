package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Page: ImageVector
    get() {
        if (_Page != null) {
            return _Page!!
        }
        _Page = ImageVector.Builder(
            name = "Page",
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
                moveTo(4f, 21.4f)
                verticalLineTo(2.6f)
                curveTo(4f, 2.269f, 4.269f, 2f, 4.6f, 2f)
                horizontalLineTo(16.251f)
                curveTo(16.411f, 2f, 16.563f, 2.063f, 16.676f, 2.176f)
                lineTo(19.824f, 5.324f)
                curveTo(19.937f, 5.437f, 20f, 5.589f, 20f, 5.749f)
                verticalLineTo(21.4f)
                curveTo(20f, 21.731f, 19.731f, 22f, 19.4f, 22f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 22f, 4f, 21.731f, 4f, 21.4f)
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
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 10f)
                horizontalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 18f)
                horizontalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 14f)
                horizontalLineTo(12f)
            }
        }.build()

        return _Page!!
    }

@Suppress("ObjectPropertyName")
private var _Page: ImageVector? = null
