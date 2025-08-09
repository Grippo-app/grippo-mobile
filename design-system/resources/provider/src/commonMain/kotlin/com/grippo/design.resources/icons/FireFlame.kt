package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FireFlame: ImageVector
    get() {
        if (_FireFlame != null) {
            return _FireFlame!!
        }
        _FireFlame = ImageVector.Builder(
            name = "FireFlame",
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
                moveTo(8f, 18f)
                curveTo(8f, 20.415f, 9.791f, 21f, 12f, 21f)
                curveTo(15.759f, 21f, 17f, 18.5f, 14.5f, 13.5f)
                curveTo(11f, 18f, 10.5f, 11f, 11f, 9f)
                curveTo(9.5f, 12f, 8f, 14.818f, 8f, 18f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                curveTo(17.049f, 21f, 20f, 18.096f, 20f, 13.125f)
                curveTo(20f, 8.154f, 12f, 3f, 12f, 3f)
                curveTo(12f, 3f, 4f, 8.154f, 4f, 13.125f)
                curveTo(4f, 18.096f, 6.951f, 21f, 12f, 21f)
                close()
            }
        }.build()

        return _FireFlame!!
    }

@Suppress("ObjectPropertyName")
private var _FireFlame: ImageVector? = null
