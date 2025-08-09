package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PageEdit: ImageVector
    get() {
        if (_PageEdit != null) {
            return _PageEdit!!
        }
        _PageEdit = ImageVector.Builder(
            name = "PageEdit",
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
                moveTo(17.954f, 16.94f)
                lineTo(19.54f, 18.525f)
                moveTo(17.954f, 16.94f)
                lineTo(18.954f, 15.939f)
                curveTo(19.392f, 15.502f, 20.102f, 15.502f, 20.54f, 15.939f)
                curveTo(20.978f, 16.377f, 20.978f, 17.087f, 20.54f, 17.525f)
                lineTo(19.54f, 18.525f)
                lineTo(17.954f, 16.94f)
                close()
                moveTo(17.954f, 16.94f)
                lineTo(14.963f, 19.931f)
                curveTo(14.813f, 20.08f, 14.715f, 20.274f, 14.682f, 20.484f)
                lineTo(14.439f, 22.04f)
                lineTo(15.996f, 21.797f)
                curveTo(16.205f, 21.765f, 16.399f, 21.666f, 16.549f, 21.516f)
                lineTo(19.54f, 18.525f)
                lineTo(17.954f, 16.94f)
                close()
            }
        }.build()

        return _PageEdit!!
    }

@Suppress("ObjectPropertyName")
private var _PageEdit: ImageVector? = null
