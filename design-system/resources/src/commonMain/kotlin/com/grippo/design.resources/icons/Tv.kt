package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Tv: ImageVector
    get() {
        if (_Tv != null) {
            return _Tv!!
        }
        _Tv = ImageVector.Builder(
            name = "Tv",
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
                moveTo(20f, 7f)
                horizontalLineTo(4f)
                curveTo(2.895f, 7f, 2f, 7.895f, 2f, 9f)
                verticalLineTo(20f)
                curveTo(2f, 21.105f, 2.895f, 22f, 4f, 22f)
                horizontalLineTo(20f)
                curveTo(21.105f, 22f, 22f, 21.105f, 22f, 20f)
                verticalLineTo(9f)
                curveTo(22f, 7.895f, 21.105f, 7f, 20f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 2f)
                lineTo(12f, 7f)
                lineTo(7f, 2f)
            }
        }.build()

        return _Tv!!
    }

@Suppress("ObjectPropertyName")
private var _Tv: ImageVector? = null
