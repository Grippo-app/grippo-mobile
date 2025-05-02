package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerLeftDown: ImageVector
    get() {
        if (_CornerLeftDown != null) {
            return _CornerLeftDown!!
        }
        _CornerLeftDown = ImageVector.Builder(
            name = "CornerLeftDown",
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
                moveTo(14f, 15f)
                lineTo(9f, 20f)
                lineTo(4f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 4f)
                horizontalLineTo(13f)
                curveTo(11.939f, 4f, 10.922f, 4.421f, 10.172f, 5.172f)
                curveTo(9.421f, 5.922f, 9f, 6.939f, 9f, 8f)
                verticalLineTo(20f)
            }
        }.build()

        return _CornerLeftDown!!
    }

@Suppress("ObjectPropertyName")
private var _CornerLeftDown: ImageVector? = null
