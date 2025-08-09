package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.OffRounded: ImageVector
    get() {
        if (_OffRounded != null) {
            return _OffRounded!!
        }
        _OffRounded = ImageVector.Builder(
            name = "OffRounded",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(1f, 15f)
                verticalLineTo(9f)
                curveTo(1f, 5.686f, 3.686f, 3f, 7f, 3f)
                horizontalLineTo(17f)
                curveTo(20.314f, 3f, 23f, 5.686f, 23f, 9f)
                verticalLineTo(15f)
                curveTo(23f, 18.314f, 20.314f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(3.686f, 21f, 1f, 18.314f, 1f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7f, 9f)
                curveTo(8.657f, 9f, 10f, 10.343f, 10f, 12f)
                curveTo(10f, 13.657f, 8.657f, 15f, 7f, 15f)
                curveTo(5.343f, 15f, 4f, 13.657f, 4f, 12f)
                curveTo(4f, 10.343f, 5.343f, 9f, 7f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12f)
                horizontalLineTo(14.571f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 12f)
                horizontalLineTo(19.571f)
            }
        }.build()

        return _OffRounded!!
    }

@Suppress("ObjectPropertyName")
private var _OffRounded: ImageVector? = null
