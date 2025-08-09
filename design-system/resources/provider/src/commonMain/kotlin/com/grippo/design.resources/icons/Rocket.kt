package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Rocket: ImageVector
    get() {
        if (_Rocket != null) {
            return _Rocket!!
        }
        _Rocket = ImageVector.Builder(
            name = "Rocket",
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
                moveTo(16.062f, 10.404f)
                lineTo(14f, 17f)
                horizontalLineTo(10f)
                lineTo(7.939f, 10.404f)
                curveTo(7.351f, 8.523f, 7.724f, 6.473f, 8.937f, 4.92f)
                lineTo(11.527f, 1.605f)
                curveTo(11.767f, 1.298f, 12.233f, 1.298f, 12.473f, 1.605f)
                lineTo(15.063f, 4.92f)
                curveTo(16.276f, 6.473f, 16.649f, 8.523f, 16.062f, 10.404f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 20f)
                curveTo(10f, 22f, 12f, 23f, 12f, 23f)
                curveTo(12f, 23f, 14f, 22f, 14f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 12.5f)
                curveTo(5f, 15f, 7f, 19f, 7f, 19f)
                lineTo(10f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.931f, 12.5f)
                curveTo(19.431f, 15f, 17.431f, 19f, 17.431f, 19f)
                lineTo(14.431f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11f)
                curveTo(10.895f, 11f, 10f, 10.105f, 10f, 9f)
                curveTo(10f, 7.895f, 10.895f, 7f, 12f, 7f)
                curveTo(13.105f, 7f, 14f, 7.895f, 14f, 9f)
                curveTo(14f, 10.105f, 13.105f, 11f, 12f, 11f)
                close()
            }
        }.build()

        return _Rocket!!
    }

@Suppress("ObjectPropertyName")
private var _Rocket: ImageVector? = null
