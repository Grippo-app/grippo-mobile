package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ColorPickerEmpty: ImageVector
    get() {
        if (_ColorPickerEmpty != null) {
            return _ColorPickerEmpty!!
        }
        _ColorPickerEmpty = ImageVector.Builder(
            name = "ColorPickerEmpty",
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
                moveTo(13.879f, 7.697f)
                lineTo(16f, 9.818f)
                curveTo(16.391f, 10.208f, 16.391f, 10.842f, 16f, 11.232f)
                lineTo(8.363f, 18.869f)
                curveTo(8.27f, 18.962f, 8.159f, 19.036f, 8.037f, 19.087f)
                lineTo(5.54f, 20.115f)
                curveTo(4.307f, 20.622f, 3.075f, 19.389f, 3.582f, 18.156f)
                lineTo(4.61f, 15.66f)
                curveTo(4.661f, 15.538f, 4.734f, 15.427f, 4.828f, 15.333f)
                lineTo(12.465f, 7.697f)
                curveTo(12.855f, 7.306f, 13.488f, 7.306f, 13.879f, 7.697f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5.575f)
                lineTo(18.121f, 7.696f)
                moveTo(13.879f, 3.454f)
                lineTo(16f, 5.575f)
                lineTo(13.879f, 3.454f)
                close()
                moveTo(20.243f, 9.818f)
                lineTo(18.121f, 7.696f)
                lineTo(20.243f, 9.818f)
                close()
                moveTo(16f, 5.575f)
                lineTo(17.414f, 4.161f)
                curveTo(17.805f, 3.77f, 18.438f, 3.77f, 18.829f, 4.161f)
                lineTo(19.536f, 4.868f)
                curveTo(19.926f, 5.259f, 19.926f, 5.892f, 19.536f, 6.282f)
                lineTo(18.121f, 7.696f)
                lineTo(16f, 5.575f)
                close()
            }
        }.build()

        return _ColorPickerEmpty!!
    }

@Suppress("ObjectPropertyName")
private var _ColorPickerEmpty: ImageVector? = null
