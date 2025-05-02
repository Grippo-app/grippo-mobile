package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Unlock: ImageVector
    get() {
        if (_Unlock != null) {
            return _Unlock!!
        }
        _Unlock = ImageVector.Builder(
            name = "Unlock",
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
                curveTo(6.999f, 5.76f, 7.458f, 4.564f, 8.289f, 3.644f)
                curveTo(9.12f, 2.724f, 10.264f, 2.145f, 11.498f, 2.02f)
                curveTo(12.731f, 1.896f, 13.967f, 2.234f, 14.965f, 2.969f)
                curveTo(15.964f, 3.705f, 16.653f, 4.785f, 16.9f, 6f)
            }
        }.build()

        return _Unlock!!
    }

@Suppress("ObjectPropertyName")
private var _Unlock: ImageVector? = null
