package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Gym: ImageVector
    get() {
        if (_Gym != null) {
            return _Gym!!
        }
        _Gym = ImageVector.Builder(
            name = "Gym",
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
                moveTo(7.4f, 7f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 7f, 4f, 7.269f, 4f, 7.6f)
                verticalLineTo(16.4f)
                curveTo(4f, 16.731f, 4.269f, 17f, 4.6f, 17f)
                horizontalLineTo(7.4f)
                curveTo(7.731f, 17f, 8f, 16.731f, 8f, 16.4f)
                verticalLineTo(7.6f)
                curveTo(8f, 7.269f, 7.731f, 7f, 7.4f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.4f, 7f)
                horizontalLineTo(16.6f)
                curveTo(16.269f, 7f, 16f, 7.269f, 16f, 7.6f)
                verticalLineTo(16.4f)
                curveTo(16f, 16.731f, 16.269f, 17f, 16.6f, 17f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 17f, 20f, 16.731f, 20f, 16.4f)
                verticalLineTo(7.6f)
                curveTo(20f, 7.269f, 19.731f, 7f, 19.4f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 14.4f)
                verticalLineTo(9.6f)
                curveTo(1f, 9.269f, 1.269f, 9f, 1.6f, 9f)
                horizontalLineTo(3.4f)
                curveTo(3.731f, 9f, 4f, 9.269f, 4f, 9.6f)
                verticalLineTo(14.4f)
                curveTo(4f, 14.731f, 3.731f, 15f, 3.4f, 15f)
                horizontalLineTo(1.6f)
                curveTo(1.269f, 15f, 1f, 14.731f, 1f, 14.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 14.4f)
                verticalLineTo(9.6f)
                curveTo(23f, 9.269f, 22.731f, 9f, 22.4f, 9f)
                horizontalLineTo(20.6f)
                curveTo(20.269f, 9f, 20f, 9.269f, 20f, 9.6f)
                verticalLineTo(14.4f)
                curveTo(20f, 14.731f, 20.269f, 15f, 20.6f, 15f)
                horizontalLineTo(22.4f)
                curveTo(22.731f, 15f, 23f, 14.731f, 23f, 14.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 12f)
                horizontalLineTo(16f)
            }
        }.build()

        return _Gym!!
    }

@Suppress("ObjectPropertyName")
private var _Gym: ImageVector? = null
