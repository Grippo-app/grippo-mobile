package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Height: ImageVector
    get() {
        if (_Height != null) {
            return _Height!!
        }
        _Height = ImageVector.Builder(
            name = "Height",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 3f)
                horizontalLineTo(15f)
                curveTo(16.105f, 3f, 17f, 3.895f, 17f, 5f)
                verticalLineTo(19f)
                curveTo(17f, 20.105f, 16.105f, 21f, 15f, 21f)
                horizontalLineTo(9f)
                curveTo(7.895f, 21f, 7f, 20.105f, 7f, 19f)
                verticalLineTo(5f)
                curveTo(7f, 3.895f, 7.895f, 3f, 9f, 3f)
                close()
                moveTo(7f, 7f)
                horizontalLineTo(11f)
                moveTo(7f, 11f)
                horizontalLineTo(10f)
                moveTo(7f, 15f)
                horizontalLineTo(11f)
                moveTo(7f, 19f)
                horizontalLineTo(10f)
            }
        }.build()

        return _Height!!
    }

@Suppress("ObjectPropertyName")
private var _Height: ImageVector? = null
