package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Watch: ImageVector
    get() {
        if (_Watch != null) {
            return _Watch!!
        }
        _Watch = ImageVector.Builder(
            name = "Watch",
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
                moveTo(12f, 19f)
                curveTo(15.866f, 19f, 19f, 15.866f, 19f, 12f)
                curveTo(19f, 8.134f, 15.866f, 5f, 12f, 5f)
                curveTo(8.134f, 5f, 5f, 8.134f, 5f, 12f)
                curveTo(5f, 15.866f, 8.134f, 19f, 12f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(12f)
                lineTo(13.5f, 13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.49f, 6.65f)
                lineTo(7.84f, 2.82f)
                curveTo(7.885f, 2.323f, 8.114f, 1.861f, 8.482f, 1.524f)
                curveTo(8.85f, 1.187f, 9.331f, 1f, 9.83f, 1f)
                horizontalLineTo(14.18f)
                curveTo(14.681f, 0.998f, 15.164f, 1.184f, 15.534f, 1.521f)
                curveTo(15.905f, 1.858f, 16.135f, 2.321f, 16.18f, 2.82f)
                lineTo(16.53f, 6.65f)
                moveTo(16.51f, 17.35f)
                lineTo(16.16f, 21.18f)
                curveTo(16.115f, 21.679f, 15.884f, 22.142f, 15.514f, 22.479f)
                curveTo(15.144f, 22.816f, 14.661f, 23.002f, 14.16f, 23f)
                horizontalLineTo(9.83f)
                curveTo(9.329f, 23.002f, 8.846f, 22.816f, 8.476f, 22.479f)
                curveTo(8.105f, 22.142f, 7.875f, 21.679f, 7.83f, 21.18f)
                lineTo(7.48f, 17.35f)
                horizontalLineTo(16.51f)
                close()
            }
        }.build()

        return _Watch!!
    }

@Suppress("ObjectPropertyName")
private var _Watch: ImageVector? = null
