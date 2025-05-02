package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FastForward: ImageVector
    get() {
        if (_FastForward != null) {
            return _FastForward!!
        }
        _FastForward = ImageVector.Builder(
            name = "FastForward",
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
                moveTo(13f, 19f)
                lineTo(22f, 12f)
                lineTo(13f, 5f)
                verticalLineTo(19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 19f)
                lineTo(11f, 12f)
                lineTo(2f, 5f)
                verticalLineTo(19f)
                close()
            }
        }.build()

        return _FastForward!!
    }

@Suppress("ObjectPropertyName")
private var _FastForward: ImageVector? = null
