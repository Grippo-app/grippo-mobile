package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Copy: ImageVector
    get() {
        if (_Copy != null) {
            return _Copy!!
        }
        _Copy = ImageVector.Builder(
            name = "Copy",
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
                moveTo(20f, 9f)
                horizontalLineTo(11f)
                curveTo(9.895f, 9f, 9f, 9.895f, 9f, 11f)
                verticalLineTo(20f)
                curveTo(9f, 21.105f, 9.895f, 22f, 11f, 22f)
                horizontalLineTo(20f)
                curveTo(21.105f, 22f, 22f, 21.105f, 22f, 20f)
                verticalLineTo(11f)
                curveTo(22f, 9.895f, 21.105f, 9f, 20f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 15f)
                horizontalLineTo(4f)
                curveTo(3.47f, 15f, 2.961f, 14.789f, 2.586f, 14.414f)
                curveTo(2.211f, 14.039f, 2f, 13.53f, 2f, 13f)
                verticalLineTo(4f)
                curveTo(2f, 3.47f, 2.211f, 2.961f, 2.586f, 2.586f)
                curveTo(2.961f, 2.211f, 3.47f, 2f, 4f, 2f)
                horizontalLineTo(13f)
                curveTo(13.53f, 2f, 14.039f, 2.211f, 14.414f, 2.586f)
                curveTo(14.789f, 2.961f, 15f, 3.47f, 15f, 4f)
                verticalLineTo(5f)
            }
        }.build()

        return _Copy!!
    }

@Suppress("ObjectPropertyName")
private var _Copy: ImageVector? = null
