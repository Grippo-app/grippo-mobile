package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MousePointer: ImageVector
    get() {
        if (_MousePointer != null) {
            return _MousePointer!!
        }
        _MousePointer = ImageVector.Builder(
            name = "MousePointer",
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
                moveTo(3f, 3f)
                lineTo(10.07f, 19.97f)
                lineTo(12.58f, 12.58f)
                lineTo(19.97f, 10.07f)
                lineTo(3f, 3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 13f)
                lineTo(19f, 19f)
            }
        }.build()

        return _MousePointer!!
    }

@Suppress("ObjectPropertyName")
private var _MousePointer: ImageVector? = null
