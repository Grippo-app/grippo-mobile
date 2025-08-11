package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.`3DArcCenterPt`: ImageVector
    get() {
        if (_3DArcCenterPt != null) {
            return _3DArcCenterPt!!
        }
        _3DArcCenterPt = ImageVector.Builder(
            name = "3DArcCenterPt",
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
                moveTo(22f, 16f)
                curveTo(22f, 10.477f, 17.523f, 6f, 12f, 6f)
                curveTo(7.899f, 6f, 4.375f, 8.468f, 2.832f, 12f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 17f)
                curveTo(2.552f, 17f, 3f, 16.552f, 3f, 16f)
                curveTo(3f, 15.448f, 2.552f, 15f, 2f, 15f)
                curveTo(1.448f, 15f, 1f, 15.448f, 1f, 16f)
                curveTo(1f, 16.552f, 1.448f, 17f, 2f, 17f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 16f)
                horizontalLineTo(12f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                curveTo(12.552f, 17f, 13f, 16.552f, 13f, 16f)
                curveTo(13f, 15.448f, 12.552f, 15f, 12f, 15f)
                curveTo(11.448f, 15f, 11f, 15.448f, 11f, 16f)
                curveTo(11f, 16.552f, 11.448f, 17f, 12f, 17f)
                close()
            }
        }.build()

        return _3DArcCenterPt!!
    }

@Suppress("ObjectPropertyName")
private var _3DArcCenterPt: ImageVector? = null
