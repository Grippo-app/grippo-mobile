package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MoreVert: ImageVector
    get() {
        if (_MoreVert != null) {
            return _MoreVert!!
        }
        _MoreVert = ImageVector.Builder(
            name = "MoreVert",
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
                moveTo(12f, 18.5f)
                curveTo(12.276f, 18.5f, 12.5f, 18.276f, 12.5f, 18f)
                curveTo(12.5f, 17.724f, 12.276f, 17.5f, 12f, 17.5f)
                curveTo(11.724f, 17.5f, 11.5f, 17.724f, 11.5f, 18f)
                curveTo(11.5f, 18.276f, 11.724f, 18.5f, 12f, 18.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6.5f)
                curveTo(12.276f, 6.5f, 12.5f, 6.276f, 12.5f, 6f)
                curveTo(12.5f, 5.724f, 12.276f, 5.5f, 12f, 5.5f)
                curveTo(11.724f, 5.5f, 11.5f, 5.724f, 11.5f, 6f)
                curveTo(11.5f, 6.276f, 11.724f, 6.5f, 12f, 6.5f)
                close()
            }
        }.build()

        return _MoreVert!!
    }

@Suppress("ObjectPropertyName")
private var _MoreVert: ImageVector? = null
