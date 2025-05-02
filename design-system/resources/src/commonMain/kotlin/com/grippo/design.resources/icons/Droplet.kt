package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Droplet: ImageVector
    get() {
        if (_Droplet != null) {
            return _Droplet!!
        }
        _Droplet = ImageVector.Builder(
            name = "Droplet",
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
                moveTo(12f, 2.69f)
                lineTo(17.66f, 8.35f)
                curveTo(18.779f, 9.469f, 19.542f, 10.894f, 19.851f, 12.446f)
                curveTo(20.16f, 13.998f, 20.002f, 15.607f, 19.396f, 17.069f)
                curveTo(18.791f, 18.531f, 17.766f, 19.781f, 16.45f, 20.66f)
                curveTo(15.134f, 21.539f, 13.587f, 22.009f, 12.005f, 22.009f)
                curveTo(10.422f, 22.009f, 8.876f, 21.539f, 7.56f, 20.66f)
                curveTo(6.244f, 19.781f, 5.219f, 18.531f, 4.613f, 17.069f)
                curveTo(4.008f, 15.607f, 3.85f, 13.998f, 4.159f, 12.446f)
                curveTo(4.468f, 10.894f, 5.231f, 9.469f, 6.35f, 8.35f)
                lineTo(12f, 2.69f)
                close()
            }
        }.build()

        return _Droplet!!
    }

@Suppress("ObjectPropertyName")
private var _Droplet: ImageVector? = null
