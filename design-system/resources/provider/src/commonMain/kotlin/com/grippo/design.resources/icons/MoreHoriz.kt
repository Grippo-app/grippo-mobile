package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MoreHoriz: ImageVector
    get() {
        if (_MoreHoriz != null) {
            return _MoreHoriz!!
        }
        _MoreHoriz = ImageVector.Builder(
            name = "MoreHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 12.5f)
                curveTo(18.276f, 12.5f, 18.5f, 12.276f, 18.5f, 12f)
                curveTo(18.5f, 11.724f, 18.276f, 11.5f, 18f, 11.5f)
                curveTo(17.724f, 11.5f, 17.5f, 11.724f, 17.5f, 12f)
                curveTo(17.5f, 12.276f, 17.724f, 12.5f, 18f, 12.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12.5f)
                curveTo(12.276f, 12.5f, 12.5f, 12.276f, 12.5f, 12f)
                curveTo(12.5f, 11.724f, 12.276f, 11.5f, 12f, 11.5f)
                curveTo(11.724f, 11.5f, 11.5f, 11.724f, 11.5f, 12f)
                curveTo(11.5f, 12.276f, 11.724f, 12.5f, 12f, 12.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 12.5f)
                curveTo(6.276f, 12.5f, 6.5f, 12.276f, 6.5f, 12f)
                curveTo(6.5f, 11.724f, 6.276f, 11.5f, 6f, 11.5f)
                curveTo(5.724f, 11.5f, 5.5f, 11.724f, 5.5f, 12f)
                curveTo(5.5f, 12.276f, 5.724f, 12.5f, 6f, 12.5f)
                close()
            }
        }.build()

        return _MoreHoriz!!
    }

@Suppress("ObjectPropertyName")
private var _MoreHoriz: ImageVector? = null
