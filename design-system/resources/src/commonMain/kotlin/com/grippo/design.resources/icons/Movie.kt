package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Movie: ImageVector
    get() {
        if (_Movie != null) {
            return _Movie!!
        }
        _Movie = ImageVector.Builder(
            name = "Movie",
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
                moveTo(7f, 8.01f)
                lineTo(7.01f, 7.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 8.01f)
                lineTo(17.01f, 7.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 12.01f)
                lineTo(7.01f, 11.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 12.01f)
                lineTo(17.01f, 11.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 16.01f)
                lineTo(7.01f, 15.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 16.01f)
                lineTo(17.01f, 15.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 22f)
                verticalLineTo(20f)
                moveTo(7f, 2f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 2f, 3f, 2.269f, 3f, 2.6f)
                verticalLineTo(21.4f)
                curveTo(3f, 21.731f, 3.269f, 22f, 3.6f, 22f)
                horizontalLineTo(7f)
                verticalLineTo(2f)
                close()
                moveTo(7f, 2f)
                verticalLineTo(4f)
                verticalLineTo(2f)
                close()
                moveTo(7f, 2f)
                horizontalLineTo(17f)
                horizontalLineTo(7f)
                close()
                moveTo(17f, 2f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 2f, 21f, 2.269f, 21f, 2.6f)
                verticalLineTo(21.4f)
                curveTo(21f, 21.731f, 20.731f, 22f, 20.4f, 22f)
                horizontalLineTo(17f)
                verticalLineTo(2f)
                close()
                moveTo(17f, 2f)
                verticalLineTo(4f)
                verticalLineTo(2f)
                close()
                moveTo(17f, 22f)
                verticalLineTo(20f)
                verticalLineTo(22f)
                close()
                moveTo(17f, 22f)
                horizontalLineTo(7f)
                horizontalLineTo(17f)
                close()
            }
        }.build()

        return _Movie!!
    }

@Suppress("ObjectPropertyName")
private var _Movie: ImageVector? = null
