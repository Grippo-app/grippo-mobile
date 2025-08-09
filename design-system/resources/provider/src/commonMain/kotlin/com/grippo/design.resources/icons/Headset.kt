package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Headset: ImageVector
    get() {
        if (_Headset != null) {
            return _Headset!!
        }
        _Headset = ImageVector.Builder(
            name = "Headset",
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
                moveTo(4f, 13.5f)
                verticalLineTo(13f)
                curveTo(4f, 8.029f, 7.582f, 4f, 12f, 4f)
                curveTo(16.418f, 4f, 20f, 8.029f, 20f, 13f)
                verticalLineTo(13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 17.438f)
                verticalLineTo(15.562f)
                curveTo(2f, 14.644f, 2.625f, 13.844f, 3.515f, 13.621f)
                lineTo(4f, 13.5f)
                lineTo(5.254f, 13.186f)
                curveTo(5.633f, 13.092f, 6f, 13.378f, 6f, 13.769f)
                verticalLineTo(19.232f)
                curveTo(6f, 19.622f, 5.633f, 19.908f, 5.254f, 19.814f)
                lineTo(3.515f, 19.379f)
                curveTo(2.625f, 19.156f, 2f, 18.356f, 2f, 17.438f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 17.438f)
                verticalLineTo(15.562f)
                curveTo(22f, 14.644f, 21.375f, 13.844f, 20.485f, 13.621f)
                lineTo(20f, 13.5f)
                lineTo(18.746f, 13.186f)
                curveTo(18.367f, 13.092f, 18f, 13.378f, 18f, 13.769f)
                verticalLineTo(19.232f)
                curveTo(18f, 19.622f, 18.367f, 19.908f, 18.746f, 19.814f)
                lineTo(20.485f, 19.379f)
                curveTo(21.375f, 19.156f, 22f, 18.356f, 22f, 17.438f)
                close()
            }
        }.build()

        return _Headset!!
    }

@Suppress("ObjectPropertyName")
private var _Headset: ImageVector? = null
