package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Exclude: ImageVector
    get() {
        if (_Exclude != null) {
            return _Exclude!!
        }
        _Exclude = ImageVector.Builder(
            name = "Exclude",
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
                moveTo(10.5f, 15f)
                horizontalLineTo(9.6f)
                curveTo(9.269f, 15f, 9f, 15.269f, 9f, 15.6f)
                verticalLineTo(20.4f)
                curveTo(9f, 20.731f, 9.269f, 21f, 9.6f, 21f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 21f, 21f, 20.731f, 21f, 20.4f)
                verticalLineTo(9.6f)
                curveTo(21f, 9.269f, 20.731f, 9f, 20.4f, 9f)
                horizontalLineTo(15.6f)
                curveTo(15.269f, 9f, 15f, 9.269f, 15f, 9.6f)
                verticalLineTo(10.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.5f, 15f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 15f, 15f, 14.731f, 15f, 14.4f)
                verticalLineTo(13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 13.5f)
                verticalLineTo(14.4f)
                curveTo(9f, 14.731f, 8.731f, 15f, 8.4f, 15f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 15f, 3f, 14.731f, 3f, 14.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 3f, 15f, 3.269f, 15f, 3.6f)
                verticalLineTo(8.4f)
                curveTo(15f, 8.731f, 14.731f, 9f, 14.4f, 9f)
                horizontalLineTo(13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 10.5f)
                verticalLineTo(9.6f)
                curveTo(9f, 9.269f, 9.269f, 9f, 9.6f, 9f)
                horizontalLineTo(10.5f)
            }
        }.build()

        return _Exclude!!
    }

@Suppress("ObjectPropertyName")
private var _Exclude: ImageVector? = null
