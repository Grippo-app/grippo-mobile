package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Rss: ImageVector
    get() {
        if (_Rss != null) {
            return _Rss!!
        }
        _Rss = ImageVector.Builder(
            name = "Rss",
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
                moveTo(4f, 11f)
                curveTo(6.387f, 11f, 8.676f, 11.948f, 10.364f, 13.636f)
                curveTo(12.052f, 15.324f, 13f, 17.613f, 13f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                curveTo(8.243f, 4f, 12.313f, 5.686f, 15.314f, 8.686f)
                curveTo(18.314f, 11.687f, 20f, 15.757f, 20f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 20f)
                curveTo(5.552f, 20f, 6f, 19.552f, 6f, 19f)
                curveTo(6f, 18.448f, 5.552f, 18f, 5f, 18f)
                curveTo(4.448f, 18f, 4f, 18.448f, 4f, 19f)
                curveTo(4f, 19.552f, 4.448f, 20f, 5f, 20f)
                close()
            }
        }.build()

        return _Rss!!
    }

@Suppress("ObjectPropertyName")
private var _Rss: ImageVector? = null
