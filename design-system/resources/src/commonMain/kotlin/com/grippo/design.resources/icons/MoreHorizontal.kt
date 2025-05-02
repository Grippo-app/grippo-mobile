package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MoreHorizontal: ImageVector
    get() {
        if (_MoreHorizontal != null) {
            return _MoreHorizontal!!
        }
        _MoreHorizontal = ImageVector.Builder(
            name = "MoreHorizontal",
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
                moveTo(12f, 13f)
                curveTo(12.552f, 13f, 13f, 12.552f, 13f, 12f)
                curveTo(13f, 11.448f, 12.552f, 11f, 12f, 11f)
                curveTo(11.448f, 11f, 11f, 11.448f, 11f, 12f)
                curveTo(11f, 12.552f, 11.448f, 13f, 12f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 13f)
                curveTo(19.552f, 13f, 20f, 12.552f, 20f, 12f)
                curveTo(20f, 11.448f, 19.552f, 11f, 19f, 11f)
                curveTo(18.448f, 11f, 18f, 11.448f, 18f, 12f)
                curveTo(18f, 12.552f, 18.448f, 13f, 19f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 13f)
                curveTo(5.552f, 13f, 6f, 12.552f, 6f, 12f)
                curveTo(6f, 11.448f, 5.552f, 11f, 5f, 11f)
                curveTo(4.448f, 11f, 4f, 11.448f, 4f, 12f)
                curveTo(4f, 12.552f, 4.448f, 13f, 5f, 13f)
                close()
            }
        }.build()

        return _MoreHorizontal!!
    }

@Suppress("ObjectPropertyName")
private var _MoreHorizontal: ImageVector? = null
