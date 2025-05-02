package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Edit3: ImageVector
    get() {
        if (_Edit3 != null) {
            return _Edit3!!
        }
        _Edit3 = ImageVector.Builder(
            name = "Edit3",
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
                moveTo(12f, 20f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 3.5f)
                curveTo(16.898f, 3.102f, 17.437f, 2.879f, 18f, 2.879f)
                curveTo(18.279f, 2.879f, 18.554f, 2.934f, 18.812f, 3.04f)
                curveTo(19.069f, 3.147f, 19.303f, 3.303f, 19.5f, 3.5f)
                curveTo(19.697f, 3.697f, 19.853f, 3.931f, 19.96f, 4.188f)
                curveTo(20.066f, 4.446f, 20.121f, 4.721f, 20.121f, 5f)
                curveTo(20.121f, 5.279f, 20.066f, 5.554f, 19.96f, 5.812f)
                curveTo(19.853f, 6.069f, 19.697f, 6.303f, 19.5f, 6.5f)
                lineTo(7f, 19f)
                lineTo(3f, 20f)
                lineTo(4f, 16f)
                lineTo(16.5f, 3.5f)
                close()
            }
        }.build()

        return _Edit3!!
    }

@Suppress("ObjectPropertyName")
private var _Edit3: ImageVector? = null
