package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Navigation: ImageVector
    get() {
        if (_Navigation != null) {
            return _Navigation!!
        }
        _Navigation = ImageVector.Builder(
            name = "Navigation",
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
                moveTo(3f, 11f)
                lineTo(22f, 2f)
                lineTo(13f, 21f)
                lineTo(11f, 13f)
                lineTo(3f, 11f)
                close()
            }
        }.build()

        return _Navigation!!
    }

@Suppress("ObjectPropertyName")
private var _Navigation: ImageVector? = null
