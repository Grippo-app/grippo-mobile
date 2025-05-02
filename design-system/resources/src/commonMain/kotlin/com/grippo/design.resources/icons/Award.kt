package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Award: ImageVector
    get() {
        if (_Award != null) {
            return _Award!!
        }
        _Award = ImageVector.Builder(
            name = "Award",
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
                moveTo(12f, 15f)
                curveTo(15.866f, 15f, 19f, 11.866f, 19f, 8f)
                curveTo(19f, 4.134f, 15.866f, 1f, 12f, 1f)
                curveTo(8.134f, 1f, 5f, 4.134f, 5f, 8f)
                curveTo(5f, 11.866f, 8.134f, 15f, 12f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.21f, 13.89f)
                lineTo(7f, 23f)
                lineTo(12f, 20f)
                lineTo(17f, 23f)
                lineTo(15.79f, 13.88f)
            }
        }.build()

        return _Award!!
    }

@Suppress("ObjectPropertyName")
private var _Award: ImageVector? = null
