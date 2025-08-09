package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Forward15Seconds: ImageVector
    get() {
        if (_Forward15Seconds != null) {
            return _Forward15Seconds!!
        }
        _Forward15Seconds = ImageVector.Builder(
            name = "Forward15Seconds",
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
                moveTo(21f, 13f)
                curveTo(21f, 17.971f, 16.971f, 22f, 12f, 22f)
                curveTo(7.029f, 22f, 3f, 17.971f, 3f, 13f)
                curveTo(3f, 8.029f, 7.029f, 4f, 12f, 4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 4f)
                lineTo(17.5f, 6f)
                moveTo(12f, 4f)
                horizontalLineTo(19.5f)
                horizontalLineTo(12f)
                close()
                moveTo(19.5f, 4f)
                lineTo(17.5f, 2f)
                lineTo(19.5f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                verticalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 9f)
                horizontalLineTo(13f)
                curveTo(12.448f, 9f, 12f, 9.448f, 12f, 10f)
                verticalLineTo(11.5f)
                curveTo(12f, 12.052f, 12.448f, 12.5f, 13f, 12.5f)
                horizontalLineTo(14f)
                curveTo(14.552f, 12.5f, 15f, 12.948f, 15f, 13.5f)
                verticalLineTo(15f)
                curveTo(15f, 15.552f, 14.552f, 16f, 14f, 16f)
                horizontalLineTo(12f)
            }
        }.build()

        return _Forward15Seconds!!
    }

@Suppress("ObjectPropertyName")
private var _Forward15Seconds: ImageVector? = null
