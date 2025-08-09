package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Heart: ImageVector
    get() {
        if (_Heart != null) {
            return _Heart!!
        }
        _Heart = ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 8.862f)
                curveTo(22f, 10.409f, 21.406f, 11.894f, 20.346f, 12.993f)
                curveTo(17.905f, 15.523f, 15.537f, 18.161f, 13.005f, 20.6f)
                curveTo(12.425f, 21.15f, 11.504f, 21.13f, 10.949f, 20.555f)
                lineTo(3.654f, 12.993f)
                curveTo(1.449f, 10.707f, 1.449f, 7.017f, 3.654f, 4.732f)
                curveTo(5.88f, 2.423f, 9.508f, 2.423f, 11.735f, 4.732f)
                lineTo(12f, 5.006f)
                lineTo(12.265f, 4.732f)
                curveTo(13.332f, 3.625f, 14.786f, 3f, 16.305f, 3f)
                curveTo(17.824f, 3f, 19.278f, 3.624f, 20.346f, 4.732f)
                curveTo(21.406f, 5.83f, 22f, 7.316f, 22f, 8.862f)
                close()
            }
        }.build()

        return _Heart!!
    }

@Suppress("ObjectPropertyName")
private var _Heart: ImageVector? = null
