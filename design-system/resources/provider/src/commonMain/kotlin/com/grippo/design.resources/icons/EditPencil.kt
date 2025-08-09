package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EditPencil: ImageVector
    get() {
        if (_EditPencil != null) {
            return _EditPencil!!
        }
        _EditPencil = ImageVector.Builder(
            name = "EditPencil",
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
                moveTo(13.021f, 5.828f)
                lineTo(17.97f, 10.778f)
                moveTo(13.021f, 5.828f)
                lineTo(15.849f, 3f)
                lineTo(20.799f, 7.95f)
                lineTo(17.97f, 10.778f)
                lineTo(13.021f, 5.828f)
                close()
                moveTo(13.021f, 5.828f)
                lineTo(3.414f, 15.435f)
                curveTo(3.226f, 15.623f, 3.121f, 15.877f, 3.121f, 16.142f)
                verticalLineTo(20.678f)
                horizontalLineTo(7.657f)
                curveTo(7.922f, 20.678f, 8.176f, 20.572f, 8.364f, 20.385f)
                lineTo(17.97f, 10.778f)
                lineTo(13.021f, 5.828f)
                close()
            }
        }.build()

        return _EditPencil!!
    }

@Suppress("ObjectPropertyName")
private var _EditPencil: ImageVector? = null
