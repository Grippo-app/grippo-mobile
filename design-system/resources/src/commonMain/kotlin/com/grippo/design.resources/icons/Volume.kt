package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Volume: ImageVector
    get() {
        if (_Volume != null) {
            return _Volume!!
        }
        _Volume = ImageVector.Builder(
            name = "Volume",
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
                moveTo(11f, 5f)
                lineTo(6f, 9f)
                horizontalLineTo(2f)
                verticalLineTo(15f)
                horizontalLineTo(6f)
                lineTo(11f, 19f)
                verticalLineTo(5f)
                close()
            }
        }.build()

        return _Volume!!
    }

@Suppress("ObjectPropertyName")
private var _Volume: ImageVector? = null
