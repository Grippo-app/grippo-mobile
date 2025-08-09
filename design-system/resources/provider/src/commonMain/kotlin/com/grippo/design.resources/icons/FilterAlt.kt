package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FilterAlt: ImageVector
    get() {
        if (_FilterAlt != null) {
            return _FilterAlt!!
        }
        _FilterAlt = ImageVector.Builder(
            name = "FilterAlt",
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
                moveTo(3f, 7f)
                horizontalLineTo(21f)
                moveTo(3f, 7f)
                verticalLineTo(4f)
                curveTo(3f, 3.448f, 3.448f, 3f, 4f, 3f)
                horizontalLineTo(20f)
                curveTo(20.552f, 3f, 21f, 3.448f, 21f, 4f)
                lineTo(21f, 7f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 7f)
                lineTo(9.651f, 12.701f)
                curveTo(9.872f, 12.891f, 10f, 13.168f, 10f, 13.46f)
                verticalLineTo(19.719f)
                curveTo(10f, 20.37f, 10.611f, 20.847f, 11.243f, 20.689f)
                lineTo(13.243f, 20.189f)
                curveTo(13.688f, 20.078f, 14f, 19.678f, 14f, 19.219f)
                verticalLineTo(13.46f)
                curveTo(14f, 13.168f, 14.127f, 12.891f, 14.349f, 12.701f)
                lineTo(21f, 7f)
                horizontalLineTo(3f)
                close()
            }
        }.build()

        return _FilterAlt!!
    }

@Suppress("ObjectPropertyName")
private var _FilterAlt: ImageVector? = null
