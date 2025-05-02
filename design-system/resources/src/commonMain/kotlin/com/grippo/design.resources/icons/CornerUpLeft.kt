package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerUpLeft: ImageVector
    get() {
        if (_CornerUpLeft != null) {
            return _CornerUpLeft!!
        }
        _CornerUpLeft = ImageVector.Builder(
            name = "CornerUpLeft",
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
                moveTo(9f, 14f)
                lineTo(4f, 9f)
                lineTo(9f, 4f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 20f)
                verticalLineTo(13f)
                curveTo(20f, 11.939f, 19.579f, 10.922f, 18.828f, 10.172f)
                curveTo(18.078f, 9.421f, 17.061f, 9f, 16f, 9f)
                horizontalLineTo(4f)
            }
        }.build()

        return _CornerUpLeft!!
    }

@Suppress("ObjectPropertyName")
private var _CornerUpLeft: ImageVector? = null
