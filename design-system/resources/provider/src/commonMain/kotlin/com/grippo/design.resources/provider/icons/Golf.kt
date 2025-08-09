package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Golf: ImageVector
    get() {
        if (_Golf != null) {
            return _Golf!!
        }
        _Golf = ImageVector.Builder(
            name = "Golf",
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
                moveTo(12f, 18f)
                verticalLineTo(12f)
                verticalLineTo(18f)
                close()
                moveTo(12f, 12f)
                verticalLineTo(3.41f)
                curveTo(12f, 2.979f, 12.44f, 2.689f, 12.836f, 2.858f)
                lineTo(21.28f, 6.477f)
                curveTo(21.755f, 6.681f, 21.768f, 7.349f, 21.302f, 7.571f)
                lineTo(12f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(15.866f, 22f, 19f, 20.433f, 19f, 18.5f)
                curveTo(19f, 16.567f, 15.866f, 15f, 12f, 15f)
                curveTo(8.134f, 15f, 5f, 16.567f, 5f, 18.5f)
                curveTo(5f, 20.433f, 8.134f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _Golf!!
    }

@Suppress("ObjectPropertyName")
private var _Golf: ImageVector? = null
