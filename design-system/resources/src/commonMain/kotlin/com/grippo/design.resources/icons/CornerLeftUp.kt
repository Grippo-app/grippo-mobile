package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerLeftUp: ImageVector
    get() {
        if (_CornerLeftUp != null) {
            return _CornerLeftUp!!
        }
        _CornerLeftUp = ImageVector.Builder(
            name = "CornerLeftUp",
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
                moveTo(14f, 9f)
                lineTo(9f, 4f)
                lineTo(4f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 20f)
                horizontalLineTo(13f)
                curveTo(11.939f, 20f, 10.922f, 19.579f, 10.172f, 18.828f)
                curveTo(9.421f, 18.078f, 9f, 17.061f, 9f, 16f)
                verticalLineTo(4f)
            }
        }.build()

        return _CornerLeftUp!!
    }

@Suppress("ObjectPropertyName")
private var _CornerLeftUp: ImageVector? = null
