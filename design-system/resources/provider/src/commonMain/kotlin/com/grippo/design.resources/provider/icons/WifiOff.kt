package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.WifiOff: ImageVector
    get() {
        if (_WifiOff != null) {
            return _WifiOff!!
        }
        _WifiOff = ImageVector.Builder(
            name = "WifiOff",
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
                moveTo(12f, 19.51f)
                lineTo(12.01f, 19.499f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 8f)
                curveTo(18.428f, 5.321f, 14.146f, 4.237f, 10f, 4.748f)
                moveTo(2f, 8f)
                curveTo(3.185f, 7.111f, 4.448f, 6.398f, 5.757f, 5.861f)
                lineTo(2f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 12f)
                curveTo(17.731f, 11.049f, 16.262f, 10.399f, 14.718f, 10.051f)
                moveTo(5f, 12f)
                curveTo(6.333f, 11f, 7.889f, 10.333f, 9.518f, 10f)
                lineTo(5f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 15.5f)
                curveTo(10.75f, 14.1f, 13.25f, 14.1f, 15.5f, 15.5f)
            }
        }.build()

        return _WifiOff!!
    }

@Suppress("ObjectPropertyName")
private var _WifiOff: ImageVector? = null
