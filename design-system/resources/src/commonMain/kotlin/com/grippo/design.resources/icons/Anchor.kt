package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Anchor: ImageVector
    get() {
        if (_Anchor != null) {
            return _Anchor!!
        }
        _Anchor = ImageVector.Builder(
            name = "Anchor",
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
                moveTo(12f, 8f)
                curveTo(13.657f, 8f, 15f, 6.657f, 15f, 5f)
                curveTo(15f, 3.343f, 13.657f, 2f, 12f, 2f)
                curveTo(10.343f, 2f, 9f, 3.343f, 9f, 5f)
                curveTo(9f, 6.657f, 10.343f, 8f, 12f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                verticalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                horizontalLineTo(2f)
                curveTo(2f, 14.652f, 3.054f, 17.196f, 4.929f, 19.071f)
                curveTo(6.804f, 20.946f, 9.348f, 22f, 12f, 22f)
                curveTo(14.652f, 22f, 17.196f, 20.946f, 19.071f, 19.071f)
                curveTo(20.946f, 17.196f, 22f, 14.652f, 22f, 12f)
                horizontalLineTo(19f)
            }
        }.build()

        return _Anchor!!
    }

@Suppress("ObjectPropertyName")
private var _Anchor: ImageVector? = null
