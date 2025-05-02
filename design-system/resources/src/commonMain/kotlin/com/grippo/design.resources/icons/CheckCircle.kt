package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CheckCircle: ImageVector
    get() {
        if (_CheckCircle != null) {
            return _CheckCircle!!
        }
        _CheckCircle = ImageVector.Builder(
            name = "CheckCircle",
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
                moveTo(22f, 11.08f)
                verticalLineTo(12f)
                curveTo(21.999f, 14.156f, 21.301f, 16.255f, 20.009f, 17.982f)
                curveTo(18.718f, 19.709f, 16.903f, 20.972f, 14.835f, 21.584f)
                curveTo(12.767f, 22.195f, 10.557f, 22.122f, 8.534f, 21.375f)
                curveTo(6.512f, 20.627f, 4.785f, 19.246f, 3.611f, 17.437f)
                curveTo(2.437f, 15.628f, 1.88f, 13.488f, 2.022f, 11.336f)
                curveTo(2.164f, 9.185f, 2.997f, 7.136f, 4.398f, 5.497f)
                curveTo(5.799f, 3.858f, 7.693f, 2.715f, 9.796f, 2.24f)
                curveTo(11.9f, 1.765f, 14.1f, 1.982f, 16.07f, 2.86f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 4f)
                lineTo(12f, 14.01f)
                lineTo(9f, 11.01f)
            }
        }.build()

        return _CheckCircle!!
    }

@Suppress("ObjectPropertyName")
private var _CheckCircle: ImageVector? = null
