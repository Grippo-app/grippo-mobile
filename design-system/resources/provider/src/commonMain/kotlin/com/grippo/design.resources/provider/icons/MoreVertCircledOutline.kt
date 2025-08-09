package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MoreVertCircledOutline: ImageVector
    get() {
        if (_MoreVertCircledOutline != null) {
            return _MoreVertCircledOutline!!
        }
        _MoreVertCircledOutline = ImageVector.Builder(
            name = "MoreVertCircledOutline",
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
                moveTo(12f, 7.5f)
                curveTo(12.276f, 7.5f, 12.5f, 7.276f, 12.5f, 7f)
                curveTo(12.5f, 6.724f, 12.276f, 6.5f, 12f, 6.5f)
                curveTo(11.724f, 6.5f, 11.5f, 6.724f, 11.5f, 7f)
                curveTo(11.5f, 7.276f, 11.724f, 7.5f, 12f, 7.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17.5f)
                curveTo(12.276f, 17.5f, 12.5f, 17.276f, 12.5f, 17f)
                curveTo(12.5f, 16.724f, 12.276f, 16.5f, 12f, 16.5f)
                curveTo(11.724f, 16.5f, 11.5f, 16.724f, 11.5f, 17f)
                curveTo(11.5f, 17.276f, 11.724f, 17.5f, 12f, 17.5f)
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
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _MoreVertCircledOutline!!
    }

@Suppress("ObjectPropertyName")
private var _MoreVertCircledOutline: ImageVector? = null
