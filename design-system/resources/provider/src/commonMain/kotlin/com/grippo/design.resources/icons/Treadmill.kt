package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Treadmill: ImageVector
    get() {
        if (_Treadmill != null) {
            return _Treadmill!!
        }
        _Treadmill = ImageVector.Builder(
            name = "Treadmill",
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
                moveTo(13f, 6f)
                curveTo(14.105f, 6f, 15f, 5.105f, 15f, 4f)
                curveTo(15f, 2.895f, 14.105f, 2f, 13f, 2f)
                curveTo(11.895f, 2f, 11f, 2.895f, 11f, 4f)
                curveTo(11f, 5.105f, 11.895f, 6f, 13f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.613f, 7.267f)
                lineTo(7.305f, 11.402f)
                lineTo(11.44f, 15.537f)
                lineTo(9.373f, 20.086f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.41f, 8.507f)
                lineTo(7.797f, 5.199f)
                lineTo(10.613f, 7.267f)
                lineTo(13.508f, 10.575f)
                horizontalLineTo(15.23f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.892f, 14.71f)
                lineTo(5.651f, 15.537f)
                horizontalLineTo(2.343f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.343f, 21.537f)
                lineTo(18.651f, 19.537f)
                verticalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.892f, 6f)
                lineTo(18.651f, 8f)
                lineTo(17f, 9.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.891f, 21.71f)
                lineTo(18.651f, 19.537f)
            }
        }.build()

        return _Treadmill!!
    }

@Suppress("ObjectPropertyName")
private var _Treadmill: ImageVector? = null
