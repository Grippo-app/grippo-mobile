package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Wifi: ImageVector
    get() {
        if (_Wifi != null) {
            return _Wifi!!
        }
        _Wifi = ImageVector.Builder(
            name = "Wifi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12.55f)
                curveTo(6.977f, 10.904f, 9.468f, 10.002f, 12.04f, 10.002f)
                curveTo(14.612f, 10.002f, 17.103f, 10.904f, 19.08f, 12.55f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1.42f, 9f)
                curveTo(4.342f, 6.424f, 8.104f, 5.003f, 12f, 5.003f)
                curveTo(15.896f, 5.003f, 19.658f, 6.424f, 22.58f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.53f, 16.11f)
                curveTo(9.545f, 15.389f, 10.76f, 15.001f, 12.005f, 15.001f)
                curveTo(13.25f, 15.001f, 14.465f, 15.389f, 15.48f, 16.11f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20f)
                horizontalLineTo(12.01f)
            }
        }.build()

        return _Wifi!!
    }

@Suppress("ObjectPropertyName")
private var _Wifi: ImageVector? = null
