package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RemovePin: ImageVector
    get() {
        if (_RemovePin != null) {
            return _RemovePin!!
        }
        _RemovePin = ImageVector.Builder(
            name = "RemovePin",
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
                moveTo(9.5f, 14.5f)
                lineTo(3f, 21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.485f, 7f)
                lineTo(15.157f, 2.671f)
                lineTo(21.006f, 8.521f)
                lineTo(16.699f, 12.175f)
                moveTo(7.676f, 7.89f)
                lineTo(6.697f, 7.788f)
                lineTo(5f, 9.485f)
                lineTo(14.192f, 18.678f)
                lineTo(15.889f, 16.981f)
                lineTo(15.788f, 16f)
                lineTo(7.676f, 7.89f)
                close()
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
        }.build()

        return _RemovePin!!
    }

@Suppress("ObjectPropertyName")
private var _RemovePin: ImageVector? = null
