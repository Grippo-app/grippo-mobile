package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.WifiError: ImageVector
    get() {
        if (_WifiError != null) {
            return _WifiError!!
        }
        _WifiError = ImageVector.Builder(
            name = "WifiError",
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
                moveTo(12f, 18.51f)
                lineTo(12.01f, 18.499f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 7f)
                curveTo(8f, 2.5f, 16f, 2.5f, 22f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 11f)
                curveTo(9f, 8f, 15f, 8f, 19f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 14.5f)
                curveTo(10.75f, 13.1f, 13.25f, 13.1f, 15.5f, 14.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.243f, 19.243f)
                lineTo(21.364f, 21.364f)
                moveTo(17.121f, 21.364f)
                lineTo(19.243f, 19.243f)
                lineTo(17.121f, 21.364f)
                close()
                moveTo(21.364f, 17.121f)
                lineTo(19.243f, 19.243f)
                lineTo(21.364f, 17.121f)
                close()
                moveTo(19.243f, 19.243f)
                lineTo(17.121f, 17.121f)
                lineTo(19.243f, 19.243f)
                close()
            }
        }.build()

        return _WifiError!!
    }

@Suppress("ObjectPropertyName")
private var _WifiError: ImageVector? = null
