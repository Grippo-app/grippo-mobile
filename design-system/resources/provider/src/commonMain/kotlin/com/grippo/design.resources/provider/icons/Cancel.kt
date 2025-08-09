package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Cancel: ImageVector
    get() {
        if (_Cancel != null) {
            return _Cancel!!
        }
        _Cancel = ImageVector.Builder(
            name = "Cancel",
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
                moveTo(12.001f, 12f)
                lineTo(17.243f, 17.243f)
                moveTo(6.758f, 17.243f)
                lineTo(12.001f, 12f)
                lineTo(6.758f, 17.243f)
                close()
                moveTo(17.243f, 6.757f)
                lineTo(12.001f, 12f)
                lineTo(17.243f, 6.757f)
                close()
                moveTo(12.001f, 12f)
                lineTo(6.758f, 6.757f)
                lineTo(12.001f, 12f)
                close()
            }
        }.build()

        return _Cancel!!
    }

@Suppress("ObjectPropertyName")
private var _Cancel: ImageVector? = null
