package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BounceLeft: ImageVector
    get() {
        if (_BounceLeft != null) {
            return _BounceLeft!!
        }
        _BounceLeft = ImageVector.Builder(
            name = "BounceLeft",
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
                moveTo(6f, 7f)
                curveTo(4.895f, 7f, 4f, 6.105f, 4f, 5f)
                curveTo(4f, 3.895f, 4.895f, 3f, 6f, 3f)
                curveTo(7.105f, 3f, 8f, 3.895f, 8f, 5f)
                curveTo(8f, 6.105f, 7.105f, 7f, 6f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 15.5f)
                curveTo(18f, 14.5f, 15.5f, 15f, 13f, 20f)
                curveTo(12.5f, 17f, 11f, 12.5f, 9.5f, 10f)
            }
        }.build()

        return _BounceLeft!!
    }

@Suppress("ObjectPropertyName")
private var _BounceLeft: ImageVector? = null
