package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PanoramaReduce: ImageVector
    get() {
        if (_PanoramaReduce != null) {
            return _PanoramaReduce!!
        }
        _PanoramaReduce = ImageVector.Builder(
            name = "PanoramaReduce",
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
                moveTo(21f, 6.862f)
                verticalLineTo(17.138f)
                curveTo(21f, 17.556f, 20.585f, 17.85f, 20.189f, 17.717f)
                curveTo(18.546f, 17.166f, 14.749f, 16f, 12f, 16f)
                curveTo(9.251f, 16f, 5.454f, 17.166f, 3.811f, 17.717f)
                curveTo(3.415f, 17.85f, 3f, 17.556f, 3f, 17.138f)
                verticalLineTo(6.862f)
                curveTo(3f, 6.444f, 3.415f, 6.15f, 3.811f, 6.283f)
                curveTo(5.454f, 6.834f, 9.251f, 8f, 12f, 8f)
                curveTo(14.749f, 8f, 18.546f, 6.834f, 20.189f, 6.283f)
                curveTo(20.585f, 6.15f, 21f, 6.444f, 21f, 6.862f)
                close()
            }
        }.build()

        return _PanoramaReduce!!
    }

@Suppress("ObjectPropertyName")
private var _PanoramaReduce: ImageVector? = null
