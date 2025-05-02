package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerRightUp: ImageVector
    get() {
        if (_CornerRightUp != null) {
            return _CornerRightUp!!
        }
        _CornerRightUp = ImageVector.Builder(
            name = "CornerRightUp",
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
                moveTo(10f, 9f)
                lineTo(15f, 4f)
                lineTo(20f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                horizontalLineTo(11f)
                curveTo(12.061f, 20f, 13.078f, 19.579f, 13.828f, 18.828f)
                curveTo(14.579f, 18.078f, 15f, 17.061f, 15f, 16f)
                verticalLineTo(4f)
            }
        }.build()

        return _CornerRightUp!!
    }

@Suppress("ObjectPropertyName")
private var _CornerRightUp: ImageVector? = null
