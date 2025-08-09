package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ColorPicker: ImageVector
    get() {
        if (_ColorPicker != null) {
            return _ColorPicker!!
        }
        _ColorPicker = ImageVector.Builder(
            name = "ColorPicker",
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
                moveTo(7f, 13.161f)
                horizontalLineTo(14.071f)
                moveTo(7f, 13.161f)
                lineTo(12.464f, 7.697f)
                curveTo(12.855f, 7.306f, 13.488f, 7.306f, 13.878f, 7.697f)
                lineTo(16f, 9.818f)
                curveTo(16.39f, 10.208f, 16.39f, 10.842f, 16f, 11.232f)
                lineTo(14.071f, 13.161f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 13.161f)
                lineTo(4.828f, 15.333f)
                curveTo(4.734f, 15.427f, 4.66f, 15.538f, 4.61f, 15.66f)
                lineTo(3.582f, 18.156f)
                curveTo(3.074f, 19.389f, 4.307f, 20.622f, 5.54f, 20.115f)
                lineTo(8.037f, 19.087f)
                curveTo(8.159f, 19.036f, 8.27f, 18.962f, 8.363f, 18.869f)
                lineTo(14.071f, 13.161f)
                horizontalLineTo(7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.999f, 5.575f)
                lineTo(18.121f, 7.697f)
                moveTo(13.878f, 3.454f)
                lineTo(15.999f, 5.575f)
                lineTo(13.878f, 3.454f)
                close()
                moveTo(20.242f, 9.818f)
                lineTo(18.121f, 7.697f)
                lineTo(20.242f, 9.818f)
                close()
                moveTo(15.999f, 5.575f)
                lineTo(17.413f, 4.161f)
                curveTo(17.804f, 3.771f, 18.437f, 3.771f, 18.828f, 4.161f)
                lineTo(19.535f, 4.868f)
                curveTo(19.925f, 5.259f, 19.925f, 5.892f, 19.535f, 6.283f)
                lineTo(18.121f, 7.697f)
                lineTo(15.999f, 5.575f)
                close()
            }
        }.build()

        return _ColorPicker!!
    }

@Suppress("ObjectPropertyName")
private var _ColorPicker: ImageVector? = null
