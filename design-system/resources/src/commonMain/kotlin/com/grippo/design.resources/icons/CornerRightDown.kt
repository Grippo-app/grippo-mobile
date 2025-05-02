package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CornerRightDown: ImageVector
    get() {
        if (_CornerRightDown != null) {
            return _CornerRightDown!!
        }
        _CornerRightDown = ImageVector.Builder(
            name = "CornerRightDown",
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
                moveTo(10f, 15f)
                lineTo(15f, 20f)
                lineTo(20f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                horizontalLineTo(11f)
                curveTo(12.061f, 4f, 13.078f, 4.421f, 13.828f, 5.172f)
                curveTo(14.579f, 5.922f, 15f, 6.939f, 15f, 8f)
                verticalLineTo(20f)
            }
        }.build()

        return _CornerRightDown!!
    }

@Suppress("ObjectPropertyName")
private var _CornerRightDown: ImageVector? = null
