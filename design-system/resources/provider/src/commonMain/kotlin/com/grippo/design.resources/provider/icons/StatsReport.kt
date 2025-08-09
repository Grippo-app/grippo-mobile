package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.StatsReport: ImageVector
    get() {
        if (_StatsReport != null) {
            return _StatsReport!!
        }
        _StatsReport = ImageVector.Builder(
            name = "StatsReport",
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
                moveTo(10f, 9f)
                horizontalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 11f)
                curveTo(14.119f, 11f, 13f, 9.881f, 13f, 8.5f)
                curveTo(13f, 7.119f, 14.119f, 6f, 15.5f, 6f)
                curveTo(16.881f, 6f, 18f, 7.119f, 18f, 8.5f)
                curveTo(18f, 9.881f, 16.881f, 11f, 15.5f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                horizontalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 18f)
                lineTo(13.5f, 15f)
                lineTo(11f, 17f)
                lineTo(6f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                close()
            }
        }.build()

        return _StatsReport!!
    }

@Suppress("ObjectPropertyName")
private var _StatsReport: ImageVector? = null
