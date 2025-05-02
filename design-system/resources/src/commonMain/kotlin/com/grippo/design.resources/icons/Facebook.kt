package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Facebook: ImageVector
    get() {
        if (_Facebook != null) {
            return _Facebook!!
        }
        _Facebook = ImageVector.Builder(
            name = "Facebook",
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
                moveTo(18f, 2f)
                horizontalLineTo(15f)
                curveTo(13.674f, 2f, 12.402f, 2.527f, 11.465f, 3.464f)
                curveTo(10.527f, 4.402f, 10f, 5.674f, 10f, 7f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                horizontalLineTo(10f)
                verticalLineTo(22f)
                horizontalLineTo(14f)
                verticalLineTo(14f)
                horizontalLineTo(17f)
                lineTo(18f, 10f)
                horizontalLineTo(14f)
                verticalLineTo(7f)
                curveTo(14f, 6.735f, 14.105f, 6.48f, 14.293f, 6.293f)
                curveTo(14.48f, 6.105f, 14.735f, 6f, 15f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(2f)
                close()
            }
        }.build()

        return _Facebook!!
    }

@Suppress("ObjectPropertyName")
private var _Facebook: ImageVector? = null
