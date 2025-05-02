package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Bookmark: ImageVector
    get() {
        if (_Bookmark != null) {
            return _Bookmark!!
        }
        _Bookmark = ImageVector.Builder(
            name = "Bookmark",
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
                moveTo(19f, 21f)
                lineTo(12f, 16f)
                lineTo(5f, 21f)
                verticalLineTo(5f)
                curveTo(5f, 4.47f, 5.211f, 3.961f, 5.586f, 3.586f)
                curveTo(5.961f, 3.211f, 6.47f, 3f, 7f, 3f)
                horizontalLineTo(17f)
                curveTo(17.53f, 3f, 18.039f, 3.211f, 18.414f, 3.586f)
                curveTo(18.789f, 3.961f, 19f, 4.47f, 19f, 5f)
                verticalLineTo(21f)
                close()
            }
        }.build()

        return _Bookmark!!
    }

@Suppress("ObjectPropertyName")
private var _Bookmark: ImageVector? = null
