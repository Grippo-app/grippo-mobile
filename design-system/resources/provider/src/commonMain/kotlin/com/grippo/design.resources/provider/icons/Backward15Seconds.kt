package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Backward15Seconds: ImageVector
    get() {
        if (_Backward15Seconds != null) {
            return _Backward15Seconds!!
        }
        _Backward15Seconds = ImageVector.Builder(
            name = "Backward15Seconds",
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
                moveTo(3f, 13f)
                curveTo(3f, 17.971f, 7.029f, 22f, 12f, 22f)
                curveTo(16.971f, 22f, 21f, 17.971f, 21f, 13f)
                curveTo(21f, 8.029f, 16.971f, 4f, 12f, 4f)
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
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 4f)
                lineTo(6.5f, 6f)
                moveTo(12f, 4f)
                horizontalLineTo(4.5f)
                horizontalLineTo(12f)
                close()
                moveTo(4.5f, 4f)
                lineTo(6.5f, 2f)
                lineTo(4.5f, 4f)
                close()
            }
        }.build()

        return _Backward15Seconds!!
    }

@Suppress("ObjectPropertyName")
private var _Backward15Seconds: ImageVector? = null
