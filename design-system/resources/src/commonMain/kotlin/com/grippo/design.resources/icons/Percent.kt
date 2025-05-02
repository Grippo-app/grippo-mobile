package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Percent: ImageVector
    get() {
        if (_Percent != null) {
            return _Percent!!
        }
        _Percent = ImageVector.Builder(
            name = "Percent",
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
                moveTo(19f, 5f)
                lineTo(5f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 9f)
                curveTo(7.881f, 9f, 9f, 7.881f, 9f, 6.5f)
                curveTo(9f, 5.119f, 7.881f, 4f, 6.5f, 4f)
                curveTo(5.119f, 4f, 4f, 5.119f, 4f, 6.5f)
                curveTo(4f, 7.881f, 5.119f, 9f, 6.5f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 20f)
                curveTo(18.881f, 20f, 20f, 18.881f, 20f, 17.5f)
                curveTo(20f, 16.119f, 18.881f, 15f, 17.5f, 15f)
                curveTo(16.119f, 15f, 15f, 16.119f, 15f, 17.5f)
                curveTo(15f, 18.881f, 16.119f, 20f, 17.5f, 20f)
                close()
            }
        }.build()

        return _Percent!!
    }

@Suppress("ObjectPropertyName")
private var _Percent: ImageVector? = null
