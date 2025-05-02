package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Lock: ImageVector
    get() {
        if (_Lock != null) {
            return _Lock!!
        }
        _Lock = ImageVector.Builder(
            name = "Lock",
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
                moveTo(19f, 11f)
                horizontalLineTo(5f)
                curveTo(3.895f, 11f, 3f, 11.895f, 3f, 13f)
                verticalLineTo(20f)
                curveTo(3f, 21.105f, 3.895f, 22f, 5f, 22f)
                horizontalLineTo(19f)
                curveTo(20.105f, 22f, 21f, 21.105f, 21f, 20f)
                verticalLineTo(13f)
                curveTo(21f, 11.895f, 20.105f, 11f, 19f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 11f)
                verticalLineTo(7f)
                curveTo(7f, 5.674f, 7.527f, 4.402f, 8.464f, 3.464f)
                curveTo(9.402f, 2.527f, 10.674f, 2f, 12f, 2f)
                curveTo(13.326f, 2f, 14.598f, 2.527f, 15.535f, 3.464f)
                curveTo(16.473f, 4.402f, 17f, 5.674f, 17f, 7f)
                verticalLineTo(11f)
            }
        }.build()

        return _Lock!!
    }

@Suppress("ObjectPropertyName")
private var _Lock: ImageVector? = null
