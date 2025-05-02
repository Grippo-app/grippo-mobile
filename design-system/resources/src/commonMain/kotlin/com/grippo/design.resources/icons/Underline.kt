package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Underline: ImageVector
    get() {
        if (_Underline != null) {
            return _Underline!!
        }
        _Underline = ImageVector.Builder(
            name = "Underline",
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
                moveTo(6f, 3f)
                verticalLineTo(10f)
                curveTo(6f, 11.591f, 6.632f, 13.117f, 7.757f, 14.243f)
                curveTo(8.883f, 15.368f, 10.409f, 16f, 12f, 16f)
                curveTo(13.591f, 16f, 15.117f, 15.368f, 16.243f, 14.243f)
                curveTo(17.368f, 13.117f, 18f, 11.591f, 18f, 10f)
                verticalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 21f)
                horizontalLineTo(20f)
            }
        }.build()

        return _Underline!!
    }

@Suppress("ObjectPropertyName")
private var _Underline: ImageVector? = null
