package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.XOctagon: ImageVector
    get() {
        if (_XOctagon != null) {
            return _XOctagon!!
        }
        _XOctagon = ImageVector.Builder(
            name = "XOctagon",
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
                moveTo(7.86f, 2f)
                horizontalLineTo(16.14f)
                lineTo(22f, 7.86f)
                verticalLineTo(16.14f)
                lineTo(16.14f, 22f)
                horizontalLineTo(7.86f)
                lineTo(2f, 16.14f)
                verticalLineTo(7.86f)
                lineTo(7.86f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 9f)
                lineTo(9f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                lineTo(15f, 15f)
            }
        }.build()

        return _XOctagon!!
    }

@Suppress("ObjectPropertyName")
private var _XOctagon: ImageVector? = null
