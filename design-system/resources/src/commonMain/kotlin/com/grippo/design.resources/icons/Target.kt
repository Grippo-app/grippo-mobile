package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Target: ImageVector
    get() {
        if (_Target != null) {
            return _Target!!
        }
        _Target = ImageVector.Builder(
            name = "Target",
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
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                curveTo(15.314f, 18f, 18f, 15.314f, 18f, 12f)
                curveTo(18f, 8.686f, 15.314f, 6f, 12f, 6f)
                curveTo(8.686f, 6f, 6f, 8.686f, 6f, 12f)
                curveTo(6f, 15.314f, 8.686f, 18f, 12f, 18f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14f)
                curveTo(13.105f, 14f, 14f, 13.105f, 14f, 12f)
                curveTo(14f, 10.895f, 13.105f, 10f, 12f, 10f)
                curveTo(10.895f, 10f, 10f, 10.895f, 10f, 12f)
                curveTo(10f, 13.105f, 10.895f, 14f, 12f, 14f)
                close()
            }
        }.build()

        return _Target!!
    }

@Suppress("ObjectPropertyName")
private var _Target: ImageVector? = null
