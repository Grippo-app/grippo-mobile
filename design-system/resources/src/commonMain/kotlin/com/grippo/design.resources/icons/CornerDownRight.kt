package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerDownRight: ImageVector
    get() {
        if (_CornerDownRight != null) {
            return _CornerDownRight!!
        }
        _CornerDownRight = ImageVector.Builder(
            name = "CornerDownRight",
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
                moveTo(15f, 10f)
                lineTo(20f, 15f)
                lineTo(15f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                verticalLineTo(11f)
                curveTo(4f, 12.061f, 4.421f, 13.078f, 5.172f, 13.828f)
                curveTo(5.922f, 14.579f, 6.939f, 15f, 8f, 15f)
                horizontalLineTo(20f)
            }
        }.build()

        return _CornerDownRight!!
    }

@Suppress("ObjectPropertyName")
private var _CornerDownRight: ImageVector? = null
