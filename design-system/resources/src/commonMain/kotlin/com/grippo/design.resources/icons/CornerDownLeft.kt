package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerDownLeft: ImageVector
    get() {
        if (_CornerDownLeft != null) {
            return _CornerDownLeft!!
        }
        _CornerDownLeft = ImageVector.Builder(
            name = "CornerDownLeft",
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
                moveTo(9f, 10f)
                lineTo(4f, 15f)
                lineTo(9f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 4f)
                verticalLineTo(11f)
                curveTo(20f, 12.061f, 19.579f, 13.078f, 18.828f, 13.828f)
                curveTo(18.078f, 14.579f, 17.061f, 15f, 16f, 15f)
                horizontalLineTo(4f)
            }
        }.build()

        return _CornerDownLeft!!
    }

@Suppress("ObjectPropertyName")
private var _CornerDownLeft: ImageVector? = null
