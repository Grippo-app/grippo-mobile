package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Feather: ImageVector
    get() {
        if (_Feather != null) {
            return _Feather!!
        }
        _Feather = ImageVector.Builder(
            name = "Feather",
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
                moveTo(20.24f, 12.24f)
                curveTo(21.366f, 11.114f, 21.998f, 9.587f, 21.998f, 7.995f)
                curveTo(21.998f, 6.403f, 21.366f, 4.876f, 20.24f, 3.75f)
                curveTo(19.114f, 2.624f, 17.587f, 1.992f, 15.995f, 1.992f)
                curveTo(14.403f, 1.992f, 12.876f, 2.624f, 11.75f, 3.75f)
                lineTo(5f, 10.5f)
                verticalLineTo(19f)
                horizontalLineTo(13.5f)
                lineTo(20.24f, 12.24f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8f)
                lineTo(2f, 22f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 15f)
                horizontalLineTo(9f)
            }
        }.build()

        return _Feather!!
    }

@Suppress("ObjectPropertyName")
private var _Feather: ImageVector? = null
