package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Filter: ImageVector
    get() {
        if (_Filter != null) {
            return _Filter!!
        }
        _Filter = ImageVector.Builder(
            name = "Filter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                curveTo(20.552f, 3f, 21f, 3.448f, 21f, 4f)
                lineTo(21f, 5.586f)
                curveTo(21f, 5.851f, 20.895f, 6.105f, 20.707f, 6.293f)
                lineTo(14.292f, 12.707f)
                curveTo(14.105f, 12.895f, 14f, 13.149f, 14f, 13.414f)
                verticalLineTo(19.719f)
                curveTo(14f, 20.37f, 13.388f, 20.847f, 12.757f, 20.689f)
                lineTo(10.757f, 20.189f)
                curveTo(10.312f, 20.078f, 10f, 19.678f, 10f, 19.219f)
                verticalLineTo(13.414f)
                curveTo(10f, 13.149f, 9.894f, 12.895f, 9.707f, 12.707f)
                lineTo(3.292f, 6.293f)
                curveTo(3.105f, 6.105f, 3f, 5.851f, 3f, 5.586f)
                verticalLineTo(4f)
                curveTo(3f, 3.448f, 3.447f, 3f, 4f, 3f)
                close()
            }
        }.build()

        return _Filter!!
    }

@Suppress("ObjectPropertyName")
private var _Filter: ImageVector? = null
