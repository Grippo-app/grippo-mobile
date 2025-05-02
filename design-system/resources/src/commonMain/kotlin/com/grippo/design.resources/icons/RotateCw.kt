package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RotateCw: ImageVector
    get() {
        if (_RotateCw != null) {
            return _RotateCw!!
        }
        _RotateCw = ImageVector.Builder(
            name = "RotateCw",
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
                moveTo(23f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.49f, 15f)
                curveTo(19.84f, 16.84f, 18.61f, 18.419f, 16.984f, 19.499f)
                curveTo(15.359f, 20.578f, 13.427f, 21.101f, 11.478f, 20.987f)
                curveTo(9.53f, 20.873f, 7.672f, 20.129f, 6.184f, 18.867f)
                curveTo(4.695f, 17.605f, 3.657f, 15.893f, 3.226f, 13.99f)
                curveTo(2.795f, 12.087f, 2.994f, 10.095f, 3.794f, 8.315f)
                curveTo(4.593f, 6.535f, 5.95f, 5.063f, 7.658f, 4.121f)
                curveTo(9.367f, 3.178f, 11.336f, 2.817f, 13.268f, 3.091f)
                curveTo(15.2f, 3.365f, 16.991f, 4.26f, 18.37f, 5.64f)
                lineTo(23f, 10f)
            }
        }.build()

        return _RotateCw!!
    }

@Suppress("ObjectPropertyName")
private var _RotateCw: ImageVector? = null
