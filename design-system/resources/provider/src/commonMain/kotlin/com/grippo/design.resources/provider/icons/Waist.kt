package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Waist: ImageVector
    get() {
        if (_Waist != null) {
            return _Waist!!
        }
        _Waist = ImageVector.Builder(
            name = "Waist",
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
                moveTo(18.4f, 4f)
                curveTo(18.4f, 4f, 16.8f, 7.751f, 16.8f, 10.857f)
                curveTo(16.8f, 11.852f, 17.141f, 12.684f, 17.6f, 13.513f)
                curveTo(18.128f, 14.467f, 18.814f, 15.416f, 19.317f, 16.604f)
                curveTo(19.716f, 17.546f, 20f, 18.637f, 20f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.6f, 4f)
                curveTo(5.6f, 4f, 7.2f, 7.751f, 7.2f, 10.857f)
                curveTo(7.2f, 11.852f, 6.859f, 12.684f, 6.4f, 13.513f)
                curveTo(5.872f, 14.467f, 5.186f, 15.416f, 4.683f, 16.604f)
                curveTo(4.284f, 17.546f, 4f, 18.637f, 4f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.4f, 13.513f)
                horizontalLineTo(17.6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.683f, 16.604f)
                curveTo(4.683f, 16.604f, 10.4f, 17.714f, 12f, 20f)
                curveTo(13.6f, 17.714f, 19.316f, 16.604f, 19.316f, 16.604f)
            }
        }.build()

        return _Waist!!
    }

@Suppress("ObjectPropertyName")
private var _Waist: ImageVector? = null
