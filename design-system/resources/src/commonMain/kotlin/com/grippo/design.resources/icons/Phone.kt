package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Phone: ImageVector
    get() {
        if (_Phone != null) {
            return _Phone!!
        }
        _Phone = ImageVector.Builder(
            name = "Phone",
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
                moveTo(22f, 16.92f)
                verticalLineTo(19.92f)
                curveTo(22.001f, 20.198f, 21.944f, 20.474f, 21.833f, 20.729f)
                curveTo(21.721f, 20.985f, 21.557f, 21.214f, 21.352f, 21.402f)
                curveTo(21.147f, 21.59f, 20.905f, 21.733f, 20.641f, 21.823f)
                curveTo(20.377f, 21.912f, 20.097f, 21.945f, 19.82f, 21.92f)
                curveTo(16.743f, 21.586f, 13.787f, 20.534f, 11.19f, 18.85f)
                curveTo(8.774f, 17.315f, 6.725f, 15.266f, 5.19f, 12.85f)
                curveTo(3.5f, 10.241f, 2.448f, 7.271f, 2.12f, 4.18f)
                curveTo(2.095f, 3.903f, 2.128f, 3.625f, 2.217f, 3.362f)
                curveTo(2.305f, 3.098f, 2.448f, 2.857f, 2.635f, 2.652f)
                curveTo(2.822f, 2.447f, 3.05f, 2.283f, 3.304f, 2.171f)
                curveTo(3.558f, 2.058f, 3.832f, 2f, 4.11f, 2f)
                horizontalLineTo(7.11f)
                curveTo(7.595f, 1.995f, 8.066f, 2.167f, 8.434f, 2.484f)
                curveTo(8.802f, 2.8f, 9.042f, 3.239f, 9.11f, 3.72f)
                curveTo(9.237f, 4.68f, 9.472f, 5.623f, 9.81f, 6.53f)
                curveTo(9.945f, 6.888f, 9.974f, 7.277f, 9.894f, 7.651f)
                curveTo(9.814f, 8.025f, 9.629f, 8.368f, 9.36f, 8.64f)
                lineTo(8.09f, 9.91f)
                curveTo(9.514f, 12.413f, 11.587f, 14.486f, 14.09f, 15.91f)
                lineTo(15.36f, 14.64f)
                curveTo(15.632f, 14.371f, 15.975f, 14.186f, 16.349f, 14.106f)
                curveTo(16.723f, 14.026f, 17.112f, 14.055f, 17.47f, 14.19f)
                curveTo(18.377f, 14.528f, 19.32f, 14.763f, 20.28f, 14.89f)
                curveTo(20.766f, 14.958f, 21.209f, 15.203f, 21.527f, 15.578f)
                curveTo(21.844f, 15.952f, 22.012f, 16.43f, 22f, 16.92f)
                close()
            }
        }.build()

        return _Phone!!
    }

@Suppress("ObjectPropertyName")
private var _Phone: ImageVector? = null
